package org.springframework.data.mybatis.id;

public class Snowflake {

	// Thu, 04 Nov 2010 01:42:54 GMT
	private final long twepoch = 1288834974657L;

	private final long workerIdBits = 5L;

	private final long datacenterIdBits = 5L;

	// 最大支持机器节点数0~31，一共32个
	private final long maxWorkerId = -1L ^ (-1L << workerIdBits);

	// 最大支持数据中心节点数0~31，一共32个
	private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);

	// 序列号12位
	private final long sequenceBits = 12L;

	// 机器节点左移12位
	private final long workerIdShift = this.sequenceBits;

	// 数据中心节点左移17位
	private final long datacenterIdShift = this.sequenceBits + workerIdBits;

	// 时间毫秒数左移22位
	private final long timestampLeftShift = this.sequenceBits + workerIdBits
			+ datacenterIdBits;

	private final long sequenceMask = -1L ^ (-1L << sequenceBits); // 4095

	private long workerId;

	private long datacenterId;

	private long sequence = 0L;

	private long lastTimestamp = -1L;

	/**
	 * 构造.
	 * @param workerId 终端ID
	 * @param datacenterId 数据中心ID
	 */
	public Snowflake(long workerId, long datacenterId) {
		if (workerId > this.maxWorkerId || workerId < 0) {
			throw new IllegalArgumentException(
					String.format("worker Id can't be greater than {} or less than 0",
							this.maxWorkerId));
		}
		if (datacenterId > this.maxDatacenterId || datacenterId < 0) {
			throw new IllegalArgumentException(
					String.format("datacenter Id can't be greater than {} or less than 0",
							this.maxDatacenterId));
		}
		this.workerId = workerId;
		this.datacenterId = datacenterId;
	}

	/**
	 * 下一个ID.
	 * @return ID
	 */
	public synchronized long nextId() {
		long timestamp = genTime();
		if (timestamp < this.lastTimestamp) {
			// 如果服务器时间有问题(时钟后退) 报错。
			throw new IllegalStateException(String.format(
					"Clock moved backwards. Refusing to generate id for {}ms",
					this.lastTimestamp - timestamp));
		}
		if (this.lastTimestamp == timestamp) {
			this.sequence = (this.sequence + 1) & this.sequenceMask;
			if (this.sequence == 0) {
				timestamp = tilNextMillis(this.lastTimestamp);
			}
		}
		else {
			this.sequence = 0L;
		}

		this.lastTimestamp = timestamp;

		return ((timestamp - twepoch) << timestampLeftShift)
				| (this.datacenterId << datacenterIdShift)
				| (this.workerId << workerIdShift) | sequence;
	}

	/**
	 * 循环等待下一个时间.
	 * @param lastTimestamp 上次记录的时间
	 * @return 下一个时间
	 */
	private long tilNextMillis(long lastTimestamp) {
		long timestamp = genTime();
		while (timestamp <= lastTimestamp) {
			timestamp = genTime();
		}
		return timestamp;
	}

	/**
	 * 生成时间戳.
	 * @return 时间戳
	 */
	private long genTime() {
		return System.currentTimeMillis();
	}

}
