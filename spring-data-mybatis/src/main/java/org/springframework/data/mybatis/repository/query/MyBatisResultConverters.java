package org.springframework.data.mybatis.repository.query;

import org.springframework.core.convert.converter.Converter;
import org.springframework.dao.CleanupFailureDataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;

final class MyBatisResultConverters {

	private MyBatisResultConverters() {}

	/**
	 * Converts the given {@link Blob} into a {@code byte[]}.
	 *
	 * @author Thomas Darimont
	 */
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

			} catch (SQLException | IOException e) {
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
