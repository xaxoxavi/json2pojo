package com.bookinadvance.utils.maven;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JType;
import org.apache.commons.lang3.ClassUtils;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.exception.GenerationException;
import org.jsonschema2pojo.rules.Rule;
import org.jsonschema2pojo.rules.RuleFactory;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Applies the "format" schema rule.
 *
 * @see <a
 *      href="http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.23">http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.23</a>
 */
public class JavaTimeFormatRule implements Rule<JType, JType> {

    public static String ISO_8601_DATE_FORMAT = "yyyy-MM-dd";
    public static String ISO_8601_TIME_FORMAT = "HH:mm:ss.SSS";
    public static String ISO_8601_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    private final RuleFactory ruleFactory;
    private final Map<String, Class<?>> formatTypeMapping;

    public JavaTimeFormatRule(RuleFactory ruleFactory) {
        this.ruleFactory = ruleFactory;
        this.formatTypeMapping = getFormatTypeMapping(ruleFactory.getGenerationConfig());
    }

    /**
     * Applies this schema rule to take the required code generation steps.
     * <p>
     * This rule maps format values to Java types. By default:
     * <ul>
     * <li>"format":"date-time" =&gt; {@link Date} or {@link LocalDateTime} (if config useJodaDates is set)
     * <li>"format":"date" =&gt; {@link String} or {@link LocalDate} (if config useJodaLocalDates is set)
     * <li>"format":"time" =&gt; {@link String} or {@link LocalTime} (if config useJodaLocalTimes is set)
     * <li>"format":"utc-millisec" =&gt; <code>long</code>
     * <li>"format":"regex" =&gt; {@link Pattern}
     * <li>"format":"color" =&gt; {@link String}
     * <li>"format":"style" =&gt; {@link String}
     * <li>"format":"phone" =&gt; {@link String}
     * <li>"format":"uri" =&gt; {@link URI}
     * <li>"format":"email" =&gt; {@link String}
     * <li>"format":"ip-address" =&gt; {@link String}
     * <li>"format":"ipv6" =&gt; {@link String}
     * <li>"format":"host-name" =&gt; {@link String}
     * <li>"format":"uuid" =&gt; {@link UUID}
     * <li>other (unrecognised format) =&gt; baseType
     * </ul>
     *
     * @param nodeName
     *            the name of the node to which this format is applied
     * @param node
     *            the format node
     * @param parent
     *            the parent node
     * @param baseType
     *            the type which which is being formatted e.g. for
     *            <code>{ "type" : "string", "format" : "uri" }</code> the
     *            baseType would be java.lang.String
     * @return the Java type that is appropriate for the format value
     */
    @Override
    public JType apply(String nodeName, JsonNode node, JsonNode parent, JType baseType, Schema schema) {

        Class<?> type = getType(node.asText());
        if (type != null) {
            JType jtype = baseType.owner().ref(type);
            if (ruleFactory.getGenerationConfig().isUsePrimitives()) {
                jtype = jtype.unboxify();
            }
            return jtype;
        } else {
            return baseType;
        }
    }

    private Class<?> getType(String format) {
        return formatTypeMapping.getOrDefault(format, null);
    }

    private static Map<String, Class<?>> getFormatTypeMapping(GenerationConfig config) {

        Map<String, Class<?>> mapping = new HashMap<>(14);
        mapping.put("date-time", getDateTimeType(config));
        mapping.put("date", getDateType(config));
        mapping.put("time", getTimeType(config));
        mapping.put("utc-millisec", Long.class);
        mapping.put("regex", Pattern.class);
        mapping.put("color", String.class);
        mapping.put("style", String.class);
        mapping.put("phone", String.class);
        mapping.put("uri", URI.class);
        mapping.put("email", String.class);
        mapping.put("ip-address", String.class);
        mapping.put("ipv6", String.class);
        mapping.put("host-name", String.class);
        mapping.put("uuid", UUID.class);

        for (Map.Entry<String, String> override : config.getFormatTypeMapping().entrySet()) {
            String format = override.getKey();
            Class<?> type = tryLoadType(override.getValue(), format);
            if (type != null) {
                mapping.put(format, type);
            }
        }

        return mapping;
    }

    private static Class<?> getDateTimeType(GenerationConfig config) {
        Class<?> type = tryLoadType(config.getDateTimeType(), "data-time");
        if (type != null) {
            return type;
        }
        return LocalDateTime.class;
    }

    private static Class<?> getDateType(GenerationConfig config) {
        Class<?> type = tryLoadType(config.getDateType(), "data");
        if (type != null) {
            return type;
        }
        return LocalDate.class;
    }

    private static Class<?> getTimeType(GenerationConfig config) {
        Class<?> type = tryLoadType(config.getTimeType(), "time");
        if (type != null) {
            return type;
        }
        return LocalTime.class;
    }

    private static Class<?> tryLoadType(String typeName, String format) {
        if (!isEmpty(typeName)) {
            try {
                Class<?> type = ClassUtils.getClass(Thread.currentThread().getContextClassLoader(), typeName);
                return type;
            }
            catch (ClassNotFoundException e) {
                throw new GenerationException(format("could not load java type %s for %s", typeName, format), e);
            }
        }
        return null;
    }

}
