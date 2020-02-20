package org.springframework.jms.connection;

import org.junit.jupiter.api.Test;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class PoolConnectionFactoryTests {
	@Test
	public void testWithConnection() throws JMSException {
		Connection con = mock(Connection.class);
		List<Connection> connections = new ArrayList<>();
		connections.add(con);
		PoolConnectionFactory poolConnectionFactory = new PoolConnectionFactory(connections);
		Connection con1 = poolConnectionFactory.createConnection();
		con1.start();
		con1.stop();
		con1.close();
		Connection con2 = poolConnectionFactory.createConnection();
		con2.start();
		con2.stop();
		con2.close();
		poolConnectionFactory.destroy();  // should trigger actual close

		verify(con, times(2)).start();
		verify(con, times(2)).stop();
		verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	public void testWithConnectionFactoryAndClientId() throws JMSException {
		ConnectionFactory cf = mock(ConnectionFactory.class);
		Connection con = mock(Connection.class);
		given(cf.createConnection()).willReturn(con);

		PoolConnectionFactory poolConnectionFactory = new PoolConnectionFactory(cf);
		poolConnectionFactory.setClientId("myId");
		Connection con1 = poolConnectionFactory.createConnection();
		Connection con2 = poolConnectionFactory.createConnection();
		con1.start();
		con2.start();
		con1.close();
		con2.close();
		poolConnectionFactory.destroy();  // should trigger actual close

		verify(con).setClientID("myId");
		verify(con).start();
		verify(con).stop();
		verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	public void testWithConnectionFactoryAndExceptionListener() throws JMSException {
		ConnectionFactory cf = mock(ConnectionFactory.class);
		Connection con = mock(Connection.class);

		ExceptionListener listener = new ChainedExceptionListener();
		given(cf.createConnection()).willReturn(con);
		given(con.getExceptionListener()).willReturn(listener);

		PoolConnectionFactory poolConnectionFactory = new PoolConnectionFactory(cf);
		poolConnectionFactory.setExceptionListener(listener);
		Connection con1 = poolConnectionFactory.createConnection();
		assertThat(con1.getExceptionListener()).isEqualTo(listener);
		con1.start();
		con1.stop();
		con1.close();
		Connection con2 = poolConnectionFactory.createConnection();
		con2.start();
		con2.stop();
		con2.close();
		poolConnectionFactory.destroy();  // should trigger actual close

		verify(con).setExceptionListener(listener);
		verify(con, times(2)).start();
		verify(con, times(2)).stop();
		verify(con).close();
	}

	@Test
	public void testWithConnectionFactoryAndReconnectOnException() throws JMSException {
		ConnectionFactory cf = mock(ConnectionFactory.class);
		TestConnection con = new TestConnection();
		given(cf.createConnection()).willReturn(con);

		PoolConnectionFactory poolConnectionFactory = new PoolConnectionFactory(cf);
		poolConnectionFactory.setReconnectOnException(true);
		Connection con1 = poolConnectionFactory.createConnection();
		assertThat(con1.getExceptionListener()).isNull();
		con1.start();
		con.getExceptionListener().onException(new JMSException(""));
		Connection con2 = poolConnectionFactory.createConnection();
		con2.start();
		poolConnectionFactory.destroy();  // should trigger actual close

		assertThat(con.getStartCount()).isEqualTo(2);
		assertThat(con.getCloseCount()).isEqualTo(2);
	}

	@Test
	public void testWithConnectionFactoryAndExceptionListenerAndReconnectOnException() throws JMSException {
		ConnectionFactory cf = mock(ConnectionFactory.class);
		TestConnection con = new TestConnection();
		given(cf.createConnection()).willReturn(con);

		TestExceptionListener listener = new TestExceptionListener();
		PoolConnectionFactory poolConnectionFactory = new PoolConnectionFactory(cf);
		poolConnectionFactory.setExceptionListener(listener);
		poolConnectionFactory.setReconnectOnException(true);
		Connection con1 = poolConnectionFactory.createConnection();
		assertThat(con1.getExceptionListener()).isSameAs(listener);
		con1.start();
		con.getExceptionListener().onException(new JMSException(""));
		Connection con2 = poolConnectionFactory.createConnection();
		con2.start();
		poolConnectionFactory.destroy();  // should trigger actual close

		assertThat(con.getStartCount()).isEqualTo(2);
		assertThat(con.getCloseCount()).isEqualTo(2);
		assertThat(listener.getCount()).isEqualTo(1);
	}

}
