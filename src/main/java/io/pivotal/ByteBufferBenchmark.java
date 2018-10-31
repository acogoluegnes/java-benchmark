/*
 * Copyright (c) 2018 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.pivotal;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@State(Scope.Thread)
public class ByteBufferBenchmark {

    final static Map<String, Function<Integer, ByteBuffer>> ALLOCATORS = new HashMap<String, Function<Integer, ByteBuffer>>() {{
        put("direct", c -> ByteBuffer.allocateDirect(c));
        put("array", c -> ByteBuffer.allocate(c));
    }};

    NoOpTcpServer server;
    ByteBufferClient client;

    @Param({"direct", "array"})
    private String allocation;

    @Param({"4096", "8192", "12288", "16384"})
    private int byteBufferSize;

    @Param({"1500", "3000", "6000", "12000", "20000"})
    private int frameSize;

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(ByteBufferBenchmark.class.getSimpleName())
                .forks(1)
                .warmupTime(TimeValue.seconds(5))
                .measurementTime(TimeValue.seconds(5))
                .build();

        new Runner(opt).run();
    }

    @Setup
    public void prepare() throws Exception {
        server = new NoOpTcpServer();
        client = new ByteBufferClient(ALLOCATORS.get(allocation), frameSize, byteBufferSize);
    }

    @Benchmark
    public void write() throws Exception {
        client.write();
    }

    @TearDown
    public void tearDown() throws Exception {
        client.close();
        server.close();
    }

}
