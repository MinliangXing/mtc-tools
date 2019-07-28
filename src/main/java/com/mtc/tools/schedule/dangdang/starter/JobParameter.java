package com.mtc.tools.schedule.dangdang.starter;

import lombok.Data;

import java.io.Serializable;

@Data
public class JobParameter implements Serializable {
	private static final long serialVersionUID = -610797345091216847L;

	/**
	 * 每次查询数据量
	 */
	private int fetchNum;
	/**
	 * 取模条件
	 */
	int shardingItem;
	/**
	 * 取模条件
	 */
	int shardingTotalCount;

	public JobParameter setShardingItem(final int shardingItem) {
		this.shardingItem = shardingItem;
		return this;
	}

	public JobParameter setShardingTotalCount(final int shardingTotalCount) {
		this.shardingTotalCount = shardingTotalCount;
		return this;
	}
}
