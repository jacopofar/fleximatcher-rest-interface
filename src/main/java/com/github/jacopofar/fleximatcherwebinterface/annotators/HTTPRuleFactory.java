package com.github.jacopofar.fleximatcherwebinterface.annotators;

import com.github.jacopofar.fleximatcher.rule.RuleFactory;
import com.github.jacopofar.fleximatcher.rules.MatchingRule;

import java.net.URL;

/**
 * Created on 2016-06-29.
 */
public class HTTPRuleFactory implements RuleFactory {

    private final URL url;

    public HTTPRuleFactory(URL url) {
        this.url = url;
    }

    @Override
    public MatchingRule getRule(String parameter) {
        return new HTTPRule(url, parameter);
    }
}
