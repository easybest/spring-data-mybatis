/*
 * Copyright 2019-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.easybest.mybatis.repository.query;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;

import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

/**
 * .
 *
 * @author Jarvis Song
 */
final class MybatisResultConverters {

	private MybatisResultConverters() {
	}

	enum BlobToByteArrayConverter implements Converter<Blob, byte[]> {

		INSTANCE;

		@Nullable
		@Override
		public byte[] convert(@Nullable Blob source) {

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

			}
			catch (SQLException | IOException ex) {
				throw new DataRetrievalFailureException("Couldn't retrieve data from blob.", ex);
			}
			finally {
				if (blobStream != null) {
					try {
						blobStream.close();
					}
					catch (IOException ex) {
						throw new DataRetrievalFailureException("Couldn't close binary stream for given blob.", ex);
					}
				}
			}

			return null;
		}

	}

}
