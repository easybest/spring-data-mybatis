/*
 *
 *   Copyright 2016 the original author or authors.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.springframework.data.mybatis.repository.query;

import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.CleanupFailureDataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;

/**
 * converters of mybatis query result.
 *
 * @author Jarvis Song
 */
final class MybatisResultConverters {

    private MybatisResultConverters() {
    }

    enum BlobToByteArrayConverter implements Converter<Blob, byte[]> {

        INSTANCE;

        @Override
        public byte[] convert(Blob source) {

            if (source == null) {
                return null;
            }

            InputStream blobStream = null;
            try {

                blobStream = source.getBinaryStream();

                if (blobStream != null) {

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    StreamUtils.copy(blobStream, baos);
                    return baos.toByteArray();
                }

            } catch (SQLException e) {
                throw new DataRetrievalFailureException("Couldn't retrieve data from blob.", e);
            } catch (IOException e) {
                throw new DataRetrievalFailureException("Couldn't retrieve data from blob.", e);
            } finally {
                if (blobStream != null) {
                    try {
                        blobStream.close();
                    } catch (IOException e) {
                        throw new CleanupFailureDataAccessException("Couldn't close binary stream for given blob.", e);
                    }
                }
            }

            return null;
        }
    }

}
