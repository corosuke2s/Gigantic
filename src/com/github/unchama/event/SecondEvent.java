package com.github.unchama.event;

import com.github.unchama.event.moduler.CustomEvent;
/**
 * @author tar0ss
 *
 */
public class SecondEvent extends CustomEvent{
	private int second;

	public SecondEvent(int second){
		this.second = second;
	}

	/**初期時間から経過した秒数を取得します．(１分毎にリセットします）
	 *
	 * @return int
	 */
	public int getSecond(){
		return second;
	}
}
