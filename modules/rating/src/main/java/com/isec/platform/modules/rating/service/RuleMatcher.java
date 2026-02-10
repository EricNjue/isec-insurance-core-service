package com.isec.platform.modules.rating.service;

import com.isec.platform.modules.rating.domain.RateRule;
import com.isec.platform.modules.rating.dto.RatingContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class RuleMatcher {

    private final ExpressionParser parser = new SpelExpressionParser();

    public boolean matches(RateRule rule, RatingContext context) {
        if (rule.getConditionExpression() == null || rule.getConditionExpression().isBlank()) {
            return true;
        }
        try {
            StandardEvaluationContext evalContext = new StandardEvaluationContext(context);
            return Boolean.TRUE.equals(parser.parseExpression(rule.getConditionExpression()).getValue(evalContext, Boolean.class));
        } catch (Exception e) {
            log.error("Error evaluating condition for rule {}: {}", rule.getId(), e.getMessage());
            return false;
        }
    }

    public Object evaluateValue(RateRule rule, RatingContext context) {
        if (rule.getValueExpression() == null || rule.getValueExpression().isBlank()) {
            return null;
        }
        try {
            StandardEvaluationContext evalContext = new StandardEvaluationContext(context);
            return parser.parseExpression(rule.getValueExpression()).getValue(evalContext);
        } catch (Exception e) {
            log.error("Error evaluating value for rule {}: {}", rule.getId(), e.getMessage());
            return null;
        }
    }
    
    public BigDecimal evaluateBigDecimal(RateRule rule, RatingContext context) {
        Object value = evaluateValue(rule, context);
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        if (value instanceof String s) return new BigDecimal(s);
        return BigDecimal.ZERO;
    }
}
