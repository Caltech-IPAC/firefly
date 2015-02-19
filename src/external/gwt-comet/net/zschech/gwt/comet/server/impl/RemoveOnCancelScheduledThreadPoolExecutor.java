/*
 * Copyright 2009 Richard Zschech.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.zschech.gwt.comet.server.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link ScheduledThreadPoolExecutor} that removes cancelled tasks from the work queue.
 * 
 * This is required until Java 7's ScheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true) is available.
 * 
 * @author Richard Zschech
 */
public class RemoveOnCancelScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {
	
	public RemoveOnCancelScheduledThreadPoolExecutor(int corePoolSize, RejectedExecutionHandler handler) {
		super(corePoolSize, handler);
	}
	
	public RemoveOnCancelScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
		super(corePoolSize, threadFactory, handler);
	}
	
	public RemoveOnCancelScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
		super(corePoolSize, threadFactory);
	}
	
	public RemoveOnCancelScheduledThreadPoolExecutor(int corePoolSize) {
		super(corePoolSize);
	}
	
	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		return wrap(super.schedule(callable, delay, unit));
	}
	
	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		return wrap(super.schedule(command, delay, unit));
	}
	
	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		return wrap(super.scheduleAtFixedRate(command, initialDelay, period, unit));
	}
	
	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		return wrap(super.scheduleWithFixedDelay(command, initialDelay, delay, unit));
	}
	
	public <V> ScheduledFuture<V> wrap(final ScheduledFuture<V> wrap) {
		return new WrapScheduledFuture<V>(wrap);
	}
	
	public class WrapScheduledFuture<V> implements ScheduledFuture<V> {
		
		private final ScheduledFuture<V> wrap;
		
		public WrapScheduledFuture(ScheduledFuture<V> wrap) {
			this.wrap = wrap;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((wrap == null) ? 0 : wrap.hashCode());
			return result;
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			WrapScheduledFuture other = (WrapScheduledFuture) obj;
			if (wrap == null) {
				if (other.wrap != null) {
					return false;
				}
			}
			else if (!wrap.equals(other.wrap)) {
				return false;
			}
			return true;
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return wrap.getDelay(unit);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public int compareTo(Delayed o) {
			return wrap.compareTo(((WrapScheduledFuture) o).wrap);
		}
		
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			boolean cancelled = wrap.cancel(mayInterruptIfRunning);
			if (cancelled) {
				getQueue().remove(wrap);
			}
			return cancelled;
		}
		
		@Override
		public V get() throws InterruptedException, ExecutionException {
			return wrap.get();
		}
		
		@Override
		public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return wrap.get(timeout, unit);
		}
		
		@Override
		public boolean isCancelled() {
			return wrap.isCancelled();
		}
		
		@Override
		public boolean isDone() {
			return wrap.isDone();
		}
	}
}
