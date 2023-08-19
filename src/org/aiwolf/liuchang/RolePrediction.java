/**
 * 
 */
package org.aiwolf.liuchang;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/**
 * @author liuch
 *
 */
public class RolePrediction {
	
	int M;
	int N;
	int role;
	
	final int MAX = 100;
	
	double [][] prob;
	
	boolean[] isfixed = new boolean[15];
	boolean updated = false;
	
	ArrayList<Integer> fixed_list;
	ArrayList<Integer> not_fixed;
	HashSet<Long> hash = new HashSet<Long>();
	List<Assignment> assignments;
	Random rnd = new Random();
	ScoreMatrix scorematrix;
	
	RolePrediction() {
		
	}
	
	RolePrediction(int _N, List<Integer> fixed, int _role) {
		N = _N;
		fixed_list = new ArrayList<Integer>(fixed);
		not_fixed = new ArrayList<Integer>();
		for(int i = 0; i < N; i++) {
			if (!fixed_list.contains(i)) {
				not_fixed.add(i);
			}
		}
		role = _role;
		assignments = new ArrayList<Assignment>();
		M = 0;
		if (N == 5) {
			M = 4;
		}
		if (N == 15) {
			M = 6;
		}
		prob = new double[N][M];
	}
	
	void recalc(ScoreMatrix scorematrix, GameState gamestate) {
		updated = false;
		for (int t = assignments.size() - 1; t >= 0; t--) {
			Assignment as = assignments.get(t);
			int x = 0;
			for (int i = 0; i < N; i++) {
				if (gamestate.agents[i].Alive && as.assignment.get(i) == Util.WEREWOLF) {
					x = 1;
				}
			}
			if (x == 0) {
				assignments.remove(t);
			}
			as.calcScore(scorematrix);
		}
		assignments.sort((a, b) -> Double.compare(a.score, b.score));
	}

	void search(ScoreMatrix scorematrix, GameState gamestate, int times) {
		updated = false;
		OUTER: for (int t = 0; t < times; t++) {
			Assignment as = new Assignment();
			if (assignments.size() == 0 || rnd.nextDouble() < 0.2) {
				as = new Assignment(N, fixed_list, role);
				for (int loop = 0; loop < 10; loop++)
					as.randomswap(not_fixed);
			}
			else {
				as.copyfrom(assignments.get(rnd.nextInt(assignments.size())));
				for (int loop = 0; loop < rnd.nextInt(3) + 1; loop++)
					as.randomswap(not_fixed);
			}
			as.calcScore(scorematrix);
			if (hash.contains(as.getHash())) {
				continue OUTER;
			}
			int x = 0;
			for (int i = 0; i < N; i++) {
				if (gamestate.agents[i].Alive && as.assignment.get(i) == Util.WEREWOLF) {
					x = 1;
				}
			}
			if (x == 0) {
				continue OUTER;
			}

			assignments.add(as);
			hash.add(as.getHash());
			assignments.sort((a, b) -> Double.compare(a.score, b.score));
			while (assignments.size() > MAX) {
				hash.remove(assignments.get(assignments.size() - 1).getHash());
				assignments.remove(assignments.size() - 1);
			}
		}
	}
	
	double getProb(int agent, int role) {
		if (!updated) {
			for (int i = 0; i < N; i++) {
				for (int j = 0; j < M; j++) {
					prob[i][j] = 0;
				}
			}
			double[] ascore = new double[assignments.size()];
			double mn = 1e9;
			if (assignments.size() > 0) {
				mn = assignments.get(0).score;
			}
			for (int i = 0; i < assignments.size(); i++) {
				mn = Math.max(mn, assignments.get(i).score);
			}

			for (int i = 0; i < assignments.size(); i++) {
				ascore[i] = assignments.get(i).score - mn;
			}
			for (int i = 0; i < assignments.size(); i++) {
				for (int j = 0; j < N; j++) {
					prob[j][assignments.get(i).assignment.get(j)] += Math.exp(-ascore[i]);
				}
			}
			for (int i = 0; i < N; i++) {
				double sum = 0;
				for (int j = 0; j < M; j++) {
					sum += prob[i][j];
				}
				if (sum > 0) {
					for (int j = 0; j < M; j++) {
						prob[i][j] /= sum;
					}
				}
			}
			updated = true;
		}
		return prob[agent][role];
	}

	double probHuman(int a) {
		double res = 0;
		if (N == 5) {
			res += getProb(a, Util.VILLAGER);
			res += getProb(a, Util.SEER);
		}
		else {
			res += getProb(a, Util.VILLAGER);
			res += getProb(a, Util.SEER);
			res += getProb(a, Util.BODYGUARD);
			res += getProb(a, Util.MEDIUM);
		}

		return res;
	}

	int predict(int role) {
		int res = -1;
		double mn = -1;
		getProb(0, 0);
		for (int i = 0; i < N; i++) {
			if (mn < prob[i][role]) {
				mn = prob[i][role];
				res = i;
			}
		}
		return res;
	}

}
