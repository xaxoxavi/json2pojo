package com.bookinadvance.utils.maven;

import com.sun.codemodel.JType;
import org.jsonschema2pojo.Annotator;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.rules.Rule;
import org.jsonschema2pojo.rules.RuleFactory;

public class CustomRuleFactory extends RuleFactory {
    public CustomRuleFactory(){
        super();
    }

    public CustomRuleFactory(GenerationConfig generationConfig, Annotator annotator, SchemaStore schemaStore) {
        super(generationConfig,annotator,schemaStore);

    }
    @Override
    public Rule<JType, JType> getFormatRule() {
        return new JavaTimeFormatRule(this);
    }
}
