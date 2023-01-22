package examplefuncsplayer;

import static org.junit.Assert.*;
import org.junit.Test;
import prisms10.util.*;


public class RobotPlayerTest {

	@Test
	public void testSanity() {
		assertEquals(2, 1+1);
		int[] testArr = {1, 3, 3, 3, 6, 9, 15};
		assertEquals(5, Randomness.upperBound(testArr, 8, 0, testArr.length));
		assertEquals(1, Randomness.upperBound(testArr, 1, 0, testArr.length));


		// test of random select
		Integer[] testArr2 = new Integer[]{0, 1, 2, 3, 4, 5, 6};
		int[] testProb = new int[]{1, 1, 1, 1, 1, 1, 1};
		int[] selCnt = new int[7];
		for (int i = 0; i < 100000; i++) {
			selCnt[Randomness.randomSelect(testArr2, testProb)]++;
		}
		for (int i = 0; i < 7; i++) {
			System.out.println(selCnt[i]);
		}
	}

}
