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

import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class ByteBufferClient {

    private final Path file;

    private final List<ByteBuffer> bbs;
    private final int contentLength;
    private SocketChannel socketChannel;

    public ByteBufferClient(Function<Integer, ByteBuffer> allocator, int frameSize, int byteBufferSize) throws Exception {
        StringBuilder content = new StringBuilder();
        while (content.length() < frameSize) {
            content.append(UUID.randomUUID().toString());
            content.append(System.getProperty("line.separator"));
        }
        this.contentLength = content.length();
        String filename = UUID.randomUUID().toString();
        file = Paths.get(System.getProperty("java.io.tmpdir"), "bb-benchmark-" + filename + ".txt");
        Files.write(file, content.toString().getBytes());

        RandomAccessFile randomAccessFile = new RandomAccessFile(file.toFile(), "r");
        FileChannel fileChannel = randomAccessFile.getChannel();

        int partitions = Double.valueOf(Math.ceil((double) content.length() / (double) byteBufferSize)).intValue();
        bbs = new ArrayList<>(partitions);
        int read = 0;
        for (int i = 0; i < partitions; i++) {
            ByteBuffer bb = allocator.apply(byteBufferSize);
            read += fileChannel.read(bb);
            bb.flip();
            bbs.add(bb);
        }
        fileChannel.close();
        if (read != content.length()) {
            throw new IllegalStateException("No all data have been read in byte buffer(s)");
        }

        socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress("localhost", 9090));
    }

    public void write() throws Exception {
        int written = 0;
        for (ByteBuffer bb : bbs) {
            while (bb.hasRemaining()) {
                written += socketChannel.write(bb);
            }
            bb.rewind();
        }
        if (written != contentLength) {
            throw new IllegalStateException("No all data have been written to the socket");
        }
    }

    public void close() throws Exception {
        socketChannel.close();
        Files.delete(file);
    }

}
