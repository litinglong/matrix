package com.silva.chetax.spring.life.test.demo.inherit;

public class Lv3 extends Lv2{
	private String markLv3;
	
	Lv3(){
		System.out.println("Lv3 non-args");
	}
	
	Lv3(String markLv1){
		System.out.println("Lv3 args");
	}
}
