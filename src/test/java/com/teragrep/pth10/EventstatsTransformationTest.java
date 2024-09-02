/*
 * Teragrep Data Processing Language (DPL) translator for Apache Spark (pth_10)
 * Copyright (C) 2019-2024 Suomen Kanuuna Oy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *
 * Additional permission under GNU Affero General Public License version 3
 * section 7
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with other code, such other code is not for that reason alone subject to any
 * of the requirements of the GNU Affero GPL version 3 as long as this Program
 * is the same Program as licensed from Suomen Kanuuna Oy without any additional
 * modifications.
 *
 * Supplemented terms under GNU Affero General Public License version 3
 * section 7
 *
 * Origin of the software must be attributed to Suomen Kanuuna Oy. Any modified
 * versions must be marked as "Modified version of" The Program.
 *
 * Names of the licensors and authors may not be used for publicity purposes.
 *
 * No rights are granted for use of trade names, trademarks, or service marks
 * which are in The Program if any.
 *
 * Licensee must indemnify licensors and authors for any liability that these
 * contractual assumptions impose on licensors and authors.
 *
 * To the extent this program is licensed as part of the Commercial versions of
 * Teragrep, the applicable Commercial License may apply to this file if you as
 * a licensee so wish it.
 */
package com.teragrep.pth10;

import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.MetadataBuilder;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the EventstatsTransformation implementation Uses streaming datasets
 * 
 * @author eemhu
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EventstatsTransformationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventstatsTransformationTest.class);
    private final String testFile = "src/test/resources/eventstatsTransformationTest_data*.json"; // * to make the path into a directory path
    private final StructType testSchema = new StructType(new StructField[] {
            new StructField("_time", DataTypes.TimestampType, false, new MetadataBuilder().build()),
            new StructField("id", DataTypes.LongType, false, new MetadataBuilder().build()),
            new StructField("_raw", DataTypes.StringType, false, new MetadataBuilder().build()),
            new StructField("index", DataTypes.StringType, false, new MetadataBuilder().build()),
            new StructField("sourcetype", DataTypes.StringType, false, new MetadataBuilder().build()),
            new StructField("host", DataTypes.StringType, false, new MetadataBuilder().build()),
            new StructField("source", DataTypes.StringType, false, new MetadataBuilder().build()),
            new StructField("partition", DataTypes.StringType, false, new MetadataBuilder().build()),
            new StructField("offset", DataTypes.LongType, false, new MetadataBuilder().build())
    });

    private StreamingTestUtil streamingTestUtil;

    @org.junit.jupiter.api.BeforeAll
    void setEnv() {
        this.streamingTestUtil = new StreamingTestUtil(this.testSchema);
        this.streamingTestUtil.setEnv();
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        this.streamingTestUtil.setUp();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        this.streamingTestUtil.tearDown();
    }

    // ----------------------------------------
    // Tests
    // ----------------------------------------
    @Test
    @DisabledIfSystemProperty(
            named = "skipSparkTest",
            matches = "true"
    ) // Standard eventstats, without wildcards in WITH-clause
    public void eventstats_test_NoByClause() {
        streamingTestUtil.performDPLTest("index=index_A | eventstats avg(offset) AS avg_offset", testFile, ds -> {
            assertEquals(
                    "[_time, id, _raw, index, sourcetype, host, source, partition, offset, avg_offset]",
                    Arrays.toString(ds.columns()), "Batch handler dataset contained an unexpected column arrangement !"
            );
            List<String> listOfOffset = ds
                    .select("avg_offset")
                    .dropDuplicates()
                    .collectAsList()
                    .stream()
                    .map(r -> r.getAs(0).toString())
                    .collect(Collectors.toList());
            assertEquals(1, listOfOffset.size());
            assertEquals("5.5", listOfOffset.get(0));
        });
    }

    @Test
    @DisabledIfSystemProperty(
            named = "skipSparkTest",
            matches = "true"
    ) // Standard eventstats, without wildcards in WITH-clause
    public void eventstats_test_WithByClause() {
        streamingTestUtil
                .performDPLTest("index=index_A | eventstats avg(offset) AS avg_offset BY sourcetype", testFile, ds -> {
                    assertEquals(
                            "[_time, id, _raw, index, sourcetype, host, source, partition, offset, avg_offset]", Arrays
                                    .toString(ds.columns()),
                            "Batch handler dataset contained an unexpected column arrangement !"
                    );
                    List<String> listOfOffset = ds
                            .select("avg_offset")
                            .dropDuplicates()
                            .collectAsList()
                            .stream()
                            .map(r -> r.getAs(0).toString())
                            .collect(Collectors.toList());
                    assertEquals(2, listOfOffset.size());
                    assertEquals("6.0", listOfOffset.get(0));
                    assertEquals("5.0", listOfOffset.get(1));
                });
    }

    @Test
    @DisabledIfSystemProperty(
            named = "skipSparkTest",
            matches = "true"
    ) // count with implied wildcard
    public void eventstats_test_count() {
        streamingTestUtil.performDPLTest("index=index_A | eventstats count", testFile, ds -> {
            assertEquals(
                    "[_time, id, _raw, index, sourcetype, host, source, partition, offset, count]",
                    Arrays.toString(ds.columns()), "Batch handler dataset contained an unexpected column arrangement !"
            );
            List<String> listOfCount = ds
                    .select("count")
                    .dropDuplicates()
                    .collectAsList()
                    .stream()
                    .map(r -> r.getAs(0).toString())
                    .collect(Collectors.toList());
            assertEquals(1, listOfCount.size());
        });
    }

    @Test
    @DisabledIfSystemProperty(
            named = "skipSparkTest",
            matches = "true"
    ) // multiple aggregation functions
    public void eventstats_test_multi() {
        streamingTestUtil
                .performDPLTest("index=index_A | eventstats count avg(offset) stdevp(offset)", testFile, ds -> {
                    assertEquals(
                            "[_time, id, _raw, index, sourcetype, host, source, partition, offset, count, avg(offset), stdevp(offset)]",
                            Arrays.toString(ds.columns()), "Batch handler dataset contained an unexpected column arrangement !"
                    );
                    List<String> listOfCount = ds
                            .select("count")
                            .dropDuplicates()
                            .collectAsList()
                            .stream()
                            .map(r -> r.getAs(0).toString())
                            .collect(Collectors.toList());
                    assertEquals(1, listOfCount.size());
                    List<String> listOfAvg = ds
                            .select("avg(offset)")
                            .dropDuplicates()
                            .collectAsList()
                            .stream()
                            .map(r -> r.getAs(0).toString())
                            .collect(Collectors.toList());
                    assertEquals(1, listOfAvg.size());
                    List<String> listOfStdevp = ds
                            .select("stdevp(offset)")
                            .dropDuplicates()
                            .collectAsList()
                            .stream()
                            .map(r -> r.getAs(0).toString())
                            .collect(Collectors.toList());
                    assertEquals(1, listOfStdevp.size());
                });
    }
}
