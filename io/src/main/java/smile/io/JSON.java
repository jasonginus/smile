/*******************************************************************************
 * Copyright (c) 2010 Haifeng Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package smile.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.type.DataType;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.type.StructType;
import smile.util.Strings;

/**
 * Reads JSON datasets.
 *
 * @author Haifeng Li
 */
public class JSON {
    /** The schema of data structure. */
    private StructType schema;
    /** Charset of file. */
    private Charset charset = StandardCharsets.UTF_8;
    /** Reads JSON files in single-line or multi-line mode. */
    private Mode mode = Mode.SINGLE_LINE;

    /** JSON files in single-line or multi-line mode. */
    public enum Mode {
        /** One JSON object per line. */
        SINGLE_LINE,

        /**
         * A JSON object may occupy multiple lines.
         * The file contains a list of objects.
         * Files will be loaded as a whole entity and cannot be split.
         */
        MULTI_LINE
    }

    /**
     * Constructor.
     */
    public JSON() {

    }

    /**
     * Sets the schema.
     * @param schema the schema of file.
     */
    public JSON schema(StructType schema) {
        this.schema = schema;
        return this;
    }

    /**
     * Sets the charset.
     * @param charset the charset of file.
     */
    public JSON charset(Charset charset) {
        this.charset = charset;
        return this;
    }

    /** Reads JSON files in single-line or multi-line mode. */
    public JSON mode(Mode mode) {
        this.mode = mode;
        return this;
    }

    /**
     * Reads a JSON file.
     * @param path a JSON file path.
     */
    public DataFrame read(Path path) throws IOException {
        return read(path, Integer.MAX_VALUE);
    }

    /**
     * Reads a limited number of records from a JSON file.
     * @param path a JSON file path.
     * @param limit reads a limited number of records.
     */
    public DataFrame read(Path path, int limit) throws IOException {
        if (schema == null) {
            // infer the schema from top 1000 objects.
            schema = inferSchema(path, Math.min(1000, limit));
        }

        List<Function<String, Object>> parser = schema.parser();
        List<Tuple> rows = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        if (mode == Mode.MULTI_LINE) {
            List<Map<String, String>> maps = objectMapper.readValue(path.toFile(), new TypeReference<List<Map<String, String>>>(){});
            for (Map<String, String> map : maps) {
                rows.add(toTuple(map, parser));
                if (rows.size() >= limit) break;
            }
        } else {
            Files.lines(path, charset).limit(limit).forEach(line -> {
                try {
                    Map<String, String> map = objectMapper.readValue(line, new TypeReference<Map<String, String>>() {});
                    rows.add(toTuple(map, parser));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        }

        return DataFrame.of(rows);
    }

    /** Converts a map to tuple. */
    private Tuple toTuple(Map<String, String> map, List<Function<String, Object>> parser) {
        Object[] row = new Object[schema.length()];
        for (int i = 0; i < row.length; i++) {
            String s = map.get(schema.field(i).name);
            if (!Strings.isNullOrEmpty(s)) {
                row[i] = parser.get(i).apply(s);
            }
        }

        return Tuple.of(row, schema);
    }

    /**
     * Infer the schema from the top n objects.
     *  - Infer type of each row.
     *  - Merge row types to find common type
     *  - String type by default.
     */
    public StructType inferSchema(Path path, int limit) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        if (mode == Mode.MULTI_LINE) {
            List<Map<String, String>> maps = objectMapper.readValue(path.toFile(), new TypeReference<List<Map<String, String>>>(){});
            for (Map<String, String> map : maps) {
                rows.add(map);
                if (rows.size() >= limit) break;
            }
        } else {
            Files.lines(path, charset).limit(limit).forEach(line -> {
                try {
                    Map<String, String> map = objectMapper.readValue(line, new TypeReference<Map<String, String>>() {});
                    rows.add(map);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        }

        if (rows.isEmpty()) {
            throw new IOException("Empty file");
        }

        Map<String, DataType> types = new HashMap<>();
        for (Map<String, String> row : rows) {
            for (Map.Entry<String, String> e : row.entrySet()) {
                String name = e.getKey();
                types.put(name, DataType.coerce(types.get(name), DataType.infer(e.getValue())));
            }
        }

        StructField[] fields = new StructField[types.size()];
        int i = 0;
        for (Map.Entry<String, DataType> type : types.entrySet()) {
            fields[i++] = new StructField(type.getKey(), type.getValue());
        }
        return DataTypes.struct(fields);
    }
}