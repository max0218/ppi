package org.sar.ppi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import org.sar.ppi.communication.Message;
import org.sar.ppi.communication.MessageHandler;
import org.sar.ppi.communication.MessageHandlerException;

/**
 * Node Process Abstract class.
 */
public abstract class NodeProcess {
	protected Infrastructure infra;
	private AtomicBoolean deployed = new AtomicBoolean(true);

	/**
	 * Setter for the field <code>infra</code>.
	 *
	 * @param infra a {@link org.sar.ppi.Infrastructure} object.
	 */
	public void setInfra(Infrastructure infra) {
		this.infra = infra;
	}

	/**
	 * Handler to process a received message.
	 *
	 * @param message the message received.
	 */
	public void processMessage(Message message) {
		//System.err.println("Starting to process a message from " + message.getIdsrc() + " to " + message.getIddest());

		Method[] methods = this.getClass().getMethods();
		for (Method method : methods) {
			Class<?>[] params = method.getParameterTypes();
			if (!method.isAnnotationPresent(MessageHandler.class)) {
				continue;
			}
			if (params.length != 1) {
				throw new MessageHandlerException(
					method.getName() + ": should only have one parameter"
				);
			}
			if (!Message.class.isAssignableFrom(params[0])) {
				throw new MessageHandlerException(
					method.getName() + ": first param must extend Message"
				);
			}
			if (!params[0].equals(message.getClass())) {
				continue;
			}
			try {
				method.invoke(this, message);
				return;
			} catch (
				InvocationTargetException | IllegalAccessException | IllegalArgumentException e
			) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Start init sequence for the current node.
	 */
	public abstract void init(String[] args);

	/**
	 * Needed for peersim. Return a new intance of the current class by default.
	 *
	 * @return a {@link java.lang.Object} object.
	 * @throws java.lang.CloneNotSupportedException if fail to instanciate a new instance.
	 */
	public Object clone() throws CloneNotSupportedException {
		try {
			return this.getClass().newInstance();
		} catch (ReflectiveOperationException e) {
			throw new CloneNotSupportedException();
		}
	}

	/**
	 * Deploy the current node so it can receive messages.
	 */
	void deploy() {
		deployed.set(true);
	}

	/**
	 * Undeploy the current node (turn it off).
	 */
	void undeploy() {
		deployed.set(false);
	}

	boolean isDeployed() {
		return deployed.get();
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "NodeProcess{" + "infra=" + infra.getId() + '}';
	}
}
