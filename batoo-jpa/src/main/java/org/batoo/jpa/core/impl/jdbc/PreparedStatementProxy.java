/*
 * Copyright (c) 2012 - Batoo Software ve Consultancy Ltd.
 * 
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.batoo.jpa.core.impl.jdbc;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.NotImplementedException;
import org.batoo.jpa.common.log.BLogger;
import org.batoo.jpa.common.log.BLoggerFactory;
import org.batoo.jpa.core.impl.manager.OperationTookLongTimeWarning;

/**
 * 
 * @author hceylan
 * @since $version
 */
public class PreparedStatementProxy implements PreparedStatement {

	/**
	 * The configuration type indicating how sql printing should be.
	 * 
	 * @author hceylan
	 * @since $version
	 */
	public static enum SqlLoggingType {
		/**
		 * SQLs are not printed.
		 */
		NONE, //

		/**
		 * SQLs are printed to the standard error.
		 */
		STDERR, //

		/**
		 * SQLs are printed to the standard output.
		 */
		STDOUT
	}

	private static final BLogger LOG = BLoggerFactory.getLogger("org.batoo.jpa.SQL");

	private static AtomicLong no = new AtomicLong(0);

	private long statementNo = -1;
	private long executionNo = -1;
	private final String sql;
	private final long slowSqlThreshold;
	private final PreparedStatement statement;

	private Object[] parameters;
	private ParameterMetaData parameterMetaData;

	private boolean debug;
	private final PrintStream sqlStream;

	/**
	 * @param sql
	 *            the SQL
	 * @param statement
	 *            the delegate statement
	 * @param slowSqlThreshold
	 *            the time to decide if SQL is deemed as slow
	 * @param sqlLoggingType
	 *            the type of the sql logging
	 * 
	 * @since $version
	 * @author hceylan
	 */
	public PreparedStatementProxy(String sql, PreparedStatement statement, long slowSqlThreshold, SqlLoggingType sqlLoggingType) {
		super();

		this.sql = sql;
		this.statement = statement;
		this.slowSqlThreshold = slowSqlThreshold;

		switch (sqlLoggingType) {
			case STDERR:
				this.sqlStream = System.err;
				break;
			case STDOUT:
				this.sqlStream = System.out;
				break;
			default:
				this.sqlStream = null;
		}

		this.debug = PreparedStatementProxy.LOG.isDebugEnabled();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void addBatch() throws SQLException {
		this.statement.addBatch();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void addBatch(String sql) throws SQLException {
		this.statement.addBatch();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void cancel() throws SQLException {
		this.statement.cancel();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void clearBatch() throws SQLException {
		this.statement.clearBatch();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void clearParameters() throws SQLException {
		this.statement.clearParameters();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void clearWarnings() throws SQLException {
		this.statement.clearWarnings();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void close() throws SQLException {
		this.statement.close();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void closeOnCompletion() throws SQLException {
		this.statement.closeOnCompletion();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public boolean execute() throws SQLException {
		return this.statement.execute();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public boolean execute(String sql) throws SQLException {
		return this.statement.execute();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
		return this.statement.execute(sql, autoGeneratedKeys);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		return this.statement.execute(sql, columnIndexes);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public boolean execute(String sql, String[] columnNames) throws SQLException {
		return this.statement.execute(sql, columnNames);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public int[] executeBatch() throws SQLException {
		return this.statement.executeBatch();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public ResultSet executeQuery() throws SQLException {
		if ((this.sqlStream == null) && !this.debug) {
			return this.statement.executeQuery();
		}

		if (this.statementNo == -1) {
			this.statementNo = PreparedStatementProxy.no.incrementAndGet();
		}

		this.executionNo++;

		PreparedStatementProxy.LOG.debug("{0}:{1} executeQuery(){2}", this.statementNo, this.executionNo,
			PreparedStatementProxy.LOG.lazyBoxed(this.sql, this.parameters));

		final long start = System.currentTimeMillis();
		try {
			return this.statement.executeQuery();
		}
		finally {
			final long time = System.currentTimeMillis() - start;

			if (time > this.slowSqlThreshold) {
				PreparedStatementProxy.LOG.warn(new OperationTookLongTimeWarning(), "{0}:{1} {2} msecs, executeQuery()", this.statementNo, this.executionNo,
					time);
			}
			else {
				PreparedStatementProxy.LOG.trace("{0}:{1} {2} msecs, executeQuery()", this.statementNo, this.executionNo, time);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public ResultSet executeQuery(String sql) throws SQLException {
		this.throwNotImplemented();
		return null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public int executeUpdate() throws SQLException {
		if ((this.sqlStream == null) && !this.debug) {
			return this.statement.executeUpdate();
		}

		if (this.statementNo == -1) {
			this.statementNo = PreparedStatementProxy.no.incrementAndGet();
		}

		this.executionNo++;

		PreparedStatementProxy.LOG.debug("{0}:{1} executeUpdate(){2}", this.statementNo, this.executionNo,
			PreparedStatementProxy.LOG.lazyBoxed(this.sql, this.parameters));
		if (this.sqlStream != null) {
			this.sqlStream.println(MessageFormat.format("{0}:{1} executeUpdate(){2}", this.statementNo, this.executionNo,
				PreparedStatementProxy.LOG.lazyBoxed(this.sql, this.parameters)));
		}

		final long start = System.currentTimeMillis();
		try {
			return this.statement.executeUpdate();
		}
		finally {
			final long time = System.currentTimeMillis() - start;
			if (time > this.slowSqlThreshold) {
				if (this.sqlStream != null) {
					this.sqlStream.println(MessageFormat.format("{0}:{1} {2} msecs, executeUpdate()", this.statementNo, this.executionNo, time));

					new OperationTookLongTimeWarning().printStackTrace(this.sqlStream);
				}

				PreparedStatementProxy.LOG.warn(new OperationTookLongTimeWarning(), "{0}:{1} {2} msecs, executeUpdate()", this.statementNo, this.executionNo,
					time);
			}
			else {
				if (this.sqlStream != null) {
					this.sqlStream.println(MessageFormat.format("{0}:{1} {2} msecs, executeUpdate()", this.statementNo, this.executionNo, time));
				}

				PreparedStatementProxy.LOG.debug("{0}:{1} {2} msecs, executeUpdate()", this.statementNo, this.executionNo, time);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public int executeUpdate(String sql) throws SQLException {
		this.throwNotImplemented();
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
		this.throwNotImplemented();
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
		this.throwNotImplemented();
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public int executeUpdate(String sql, String[] columnNames) throws SQLException {
		this.throwNotImplemented();
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public Connection getConnection() throws SQLException {
		this.throwNotImplemented();
		return null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public int getFetchDirection() throws SQLException {
		this.throwNotImplemented();
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public int getFetchSize() throws SQLException {
		this.throwNotImplemented();
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public ResultSet getGeneratedKeys() throws SQLException {
		this.throwNotImplemented();
		return null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public int getMaxFieldSize() throws SQLException {
		this.throwNotImplemented();
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public int getMaxRows() throws SQLException {
		this.throwNotImplemented();
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		this.throwNotImplemented();
		return null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public boolean getMoreResults() throws SQLException {
		this.throwNotImplemented();
		return false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public boolean getMoreResults(int current) throws SQLException {
		this.throwNotImplemented();
		return false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public ParameterMetaData getParameterMetaData() throws SQLException {
		if (this.parameterMetaData != null) {
			return this.parameterMetaData;
		}

		this.parameterMetaData = this.statement.getParameterMetaData();

		if (this.parameters == null) {
			this.parameters = new Object[this.parameterMetaData.getParameterCount()];
		}

		return this.parameterMetaData;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public int getQueryTimeout() throws SQLException {
		this.throwNotImplemented();
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public ResultSet getResultSet() throws SQLException {
		this.throwNotImplemented();
		return null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public int getResultSetConcurrency() throws SQLException {
		this.throwNotImplemented();
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public int getResultSetHoldability() throws SQLException {
		this.throwNotImplemented();
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public int getResultSetType() throws SQLException {
		this.throwNotImplemented();
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public int getUpdateCount() throws SQLException {
		this.throwNotImplemented();
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public SQLWarning getWarnings() throws SQLException {
		this.throwNotImplemented();
		return null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public boolean isClosed() throws SQLException {
		this.throwNotImplemented();
		return false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public boolean isCloseOnCompletion() throws SQLException {
		this.throwNotImplemented();
		return false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public boolean isPoolable() throws SQLException {
		this.throwNotImplemented();
		return false;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		this.throwNotImplemented();
		return false;
	}

	/**
	 * Resets the prepared statement and returns itself
	 * 
	 * @return self
	 * 
	 * @since $version
	 * @author hceylan
	 */
	public PreparedStatement reset() {
		this.debug = PreparedStatementProxy.LOG.isDebugEnabled();

		return this;
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setArray(int parameterIndex, Array x) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setBlob(int parameterIndex, Blob x) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setBoolean(int parameterIndex, boolean x) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setByte(int parameterIndex, byte x) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setBytes(int parameterIndex, byte[] x) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setClob(int parameterIndex, Clob x) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setClob(int parameterIndex, Reader reader) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setCursorName(String name) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setDate(int parameterIndex, Date x) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setDouble(int parameterIndex, double x) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setEscapeProcessing(boolean enable) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setFetchDirection(int direction) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setFetchSize(int rows) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setFloat(int parameterIndex, float x) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setInt(int parameterIndex, int x) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setLong(int parameterIndex, long x) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setMaxFieldSize(int max) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setMaxRows(int max) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setNClob(int parameterIndex, NClob value) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setNClob(int parameterIndex, Reader reader) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setNString(int parameterIndex, String value) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setNull(int parameterIndex, int sqlType) throws SQLException {
		if ((this.debug || (this.sqlStream != null)) && (this.parameters != null)) {
			this.parameters[parameterIndex - 1] = null;
		}

		this.statement.setNull(parameterIndex, sqlType);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setObject(int parameterIndex, Object x) throws SQLException {
		if ((this.debug || (this.sqlStream != null)) && (this.parameters != null)) {
			this.parameters[parameterIndex - 1] = x;
		}

		this.statement.setObject(parameterIndex, x);
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setPoolable(boolean poolable) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setQueryTimeout(int seconds) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setRef(int parameterIndex, Ref x) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setRowId(int parameterIndex, RowId x) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setShort(int parameterIndex, short x) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setString(int parameterIndex, String x) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setTime(int parameterIndex, Time x) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
		this.throwNotImplemented();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public void setURL(int parameterIndex, URL x) throws SQLException {
		this.throwNotImplemented();
	}

	private void throwNotImplemented() {
		throw new NotImplementedException();
	}

	/**
	 * {@inheritDoc}
	 * 
	 */
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		this.throwNotImplemented();
		return null;
	}
}
