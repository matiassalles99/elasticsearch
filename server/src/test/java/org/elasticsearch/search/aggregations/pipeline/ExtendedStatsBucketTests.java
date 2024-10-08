/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the "Elastic License
 * 2.0", the "GNU Affero General Public License v3.0 only", and the "Server Side
 * Public License v 1"; you may not use this file except in compliance with, at
 * your election, the "Elastic License 2.0", the "GNU Affero General Public
 * License v3.0 only", or the "Server Side Public License, v 1".
 */

package org.elasticsearch.search.aggregations.pipeline;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.global.GlobalAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.support.ValueType;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class ExtendedStatsBucketTests extends AbstractBucketMetricsTestCase<ExtendedStatsBucketPipelineAggregationBuilder> {

    @Override
    protected ExtendedStatsBucketPipelineAggregationBuilder doCreateTestAggregatorFactory(String name, String bucketsPath) {
        ExtendedStatsBucketPipelineAggregationBuilder factory = new ExtendedStatsBucketPipelineAggregationBuilder(name, bucketsPath);
        if (randomBoolean()) {
            factory.sigma(randomDoubleBetween(0.0, 10.0, false));
        }
        return factory;
    }

    public void testSigmaFromInt() throws Exception {
        XContentBuilder content = XContentFactory.jsonBuilder()
            .startObject()
            .startObject("name")
            .startObject("extended_stats_bucket")
            .field("sigma", 5)
            .field("buckets_path", "test")
            .endObject()
            .endObject()
            .endObject();

        ExtendedStatsBucketPipelineAggregationBuilder builder = (ExtendedStatsBucketPipelineAggregationBuilder) parse(
            createParser(content)
        );

        assertThat(builder.sigma(), equalTo(5.0));
    }

    public void testValidate() {
        AggregationBuilder singleBucketAgg = new GlobalAggregationBuilder("global");
        AggregationBuilder multiBucketAgg = new TermsAggregationBuilder("terms").userValueTypeHint(ValueType.STRING);
        final Set<AggregationBuilder> aggBuilders = new HashSet<>();
        aggBuilders.add(singleBucketAgg);
        aggBuilders.add(multiBucketAgg);

        // First try to point to a non-existent agg
        assertThat(
            validate(aggBuilders, new ExtendedStatsBucketPipelineAggregationBuilder("name", "invalid_agg>metric")),
            equalTo(
                "Validation Failed: 1: "
                    + PipelineAggregator.Parser.BUCKETS_PATH.getPreferredName()
                    + " aggregation does not exist for aggregation [name]: invalid_agg>metric;"
            )
        );

        // Now try to point to a single bucket agg
        assertThat(
            validate(aggBuilders, new ExtendedStatsBucketPipelineAggregationBuilder("name", "global>metric")),
            equalTo(
                "Validation Failed: 1: Unable to find unqualified multi-bucket aggregation in "
                    + PipelineAggregator.Parser.BUCKETS_PATH.getPreferredName()
                    + ". Path must include a multi-bucket aggregation for aggregation [name] found :"
                    + GlobalAggregationBuilder.class.getName()
                    + " for buckets path: global>metric;"
            )
        );

        // Now try to point to a valid multi-bucket agg
        assertThat(validate(aggBuilders, new ExtendedStatsBucketPipelineAggregationBuilder("name", "terms>metric")), nullValue());
    }
}
