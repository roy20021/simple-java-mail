package org.simplejavamail.converter.internal.mimemessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jakarta.activation.DataSource;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class MimeDataSource implements Comparable<MimeDataSource>, DataSource {
	
	@Setter
	private String name;
	
	private final DataSource dataSource;
	
	@Nullable
	private final String contentDescription;
	
	@Nullable
	private final String contentTransferEncoding;

	@Override
	public int compareTo(@NotNull final MimeDataSource o) {
		int keyComparison = getName().compareTo(o.getName());
		if (keyComparison != 0) {
			return keyComparison;
		}
		return Integer.compare(getDataSource().hashCode(), o.getDataSource().hashCode());
	}

	@Override
	public boolean equals(final Object o) {
		return this == o || (o instanceof MimeDataSource && compareTo((MimeDataSource) o) == 0);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, dataSource);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return dataSource.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return dataSource.getOutputStream();
	}

	@Override
	public String getContentType() {
		return dataSource.getContentType();
	}
}