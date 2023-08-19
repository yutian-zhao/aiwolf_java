/**
 * 
 */
package org.aiwolf.liuchang;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import org.aiwolf.client.lib.AttackContentBuilder;
import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.GuardedAgentContentBuilder;
import org.aiwolf.client.lib.IdentContentBuilder;
import org.aiwolf.client.lib.RequestContentBuilder;
import org.aiwolf.client.lib.VoteContentBuilder;

import java.util.ArrayList;

/**
 * @author liuch
 *
 */
public class HowlsWerewolf extends HowlsBasePlayer {
	
	int black_count = 0;
	int co_count = 3;
	int[] divined;
	int votecnt = 0;
	int whisper_called = -1;
	
	ArrayList<ArrayList<Double>> taikou;
	StateHolder sh2;
	Parameters params;
	
	boolean[] guarded;
	boolean alive_bodyguard = true;
	boolean attack_houkoku = false;
	boolean co_whisper = false;
	boolean check_nakamaseer = false;
	double ContentRand;
	boolean f = true;
	boolean bodyguard = false;
	boolean medium = false;
	boolean seer = false;
	boolean doCO = false;
	boolean houkoku = true;
	boolean pos = false;
	boolean[] nakama;
	boolean update_sh = true;
	boolean kyoujin_ikiteru = false;

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		
		super.initialize(gameInfo, gameSetting);
		
		if (f) {
			params = new Parameters(numAgents);
			sh = new StateHolder(numAgents);
			sh2 = new StateHolder(numAgents);
			f = false;
		}
		
		update_sh = true;
		doCO = false;
		houkoku = true;
		pos = false;
		kyoujin_ikiteru = false;
		divined = new int[numAgents];
		
		for (int i = 0; i < numAgents; i++) {
			divined[i] = 0;
		}

		ArrayList<Integer> fixed = new ArrayList<Integer>();
		nakama = new boolean[numAgents];
		for (int i = 0; i < numAgents; i++)
			nakama[i] = false;
		for (Agent a : gameInfo.getRoleMap().keySet()) {
			fixed.add(a.getAgentIdx() - 1);
			nakama[a.getAgentIdx() - 1] = true;
		}
		
		sh.process(params, gamedata); 
		sh2.process(params, gamedata);
		gamedata.clear();
		sh.head = 0;
		sh2.head = 0;
		sh.game_init(fixed, meint, numAgents, Util.WEREWOLF, params);
		fixed.clear();
		fixed.add(meint);
		
		if (gamecount == 0) {
			seer = true;
		}
		else if (rnd.nextDouble() > seerExecuted / gamecount) {
			seer = true;
		}
		else {
			seer = false;
		}

		if (seer) {
			sh2.game_init(fixed, meint, numAgents, Util.SEER, params);
		}
		else {
			sh2.game_init(fixed, meint, numAgents, Util.VILLAGER, params);
		}

		if (numAgents == 15) {
			bodyguard = false;
			if (rnd.nextDouble() < 0.3) {
				medium = true;
			}
			else {
				medium = false;
			}
			black_count = 0;
			alive_bodyguard = true;
			guarded = new boolean[numAgents];
			for (int i = 0; i < numAgents; i++) {
				guarded[i] = false;
			}
			check_nakamaseer = false;
		}
		
	}

	@Override
	public void dayStart() {
		super.dayStart();
		houkoku = false;
		votecnt = 0;
		if (numAgents == 15) {
			attack_houkoku = false;
			whisper_called = -1;
			co_whisper = false;
			taikou = new ArrayList<ArrayList<Double>>();
			if (day >= 2) {
				if (currentGameInfo.getLastDeadAgentList().isEmpty()) {
					guarded[currentGameInfo.getAttackedAgent().getAgentIdx() - 1] = true;
					alive_bodyguard = true;
					System.out.println("guarded " + currentGameInfo.getAttackedAgent().toString());
				}
			}
		}
	}

	@Override
	public String whisper() {
		if (day > 0) {
			if (!attack_houkoku) {
				attack_houkoku = true;
				sh.process(params, gamedata);
				sh.update();
				int c = 0;
				ArrayList<ArrayList<Double>> votelist = new ArrayList<ArrayList<Double>>();
				if (alive_bodyguard) {
					for (int i = 0; i < numAgents; i++) {
						if (i != meint && !nakama[i]) {
							if (agents[i].COrole == Role.BODYGUARD) {
								if (sh.gamestate.agents[i].Alive) {
									whisper_called = i;
									return (new Content(new AttackContentBuilder(currentGameInfo.getAgentList().get(i)))).getText();
								}
								else {
									alive_bodyguard = false;
								}
							}
						}
					}
				}
				for (int i = 0; i < numAgents; i++) {
					if (i != meint && !nakama[i]) {
						if (sh.gamestate.agents[i].Alive) {
							double score = 1 - getPred(i, Util.POSSESSED);
							score += 0.2 * getPred(i, Util.SEER);
							score += 0.1 * getPred(i, Util.BODYGUARD);
							score += 0.1 * getPred(i, Util.MEDIUM);
							score += 3 * wincnt[i] / (gamecount + 0.01);
							ArrayList<Double> data = new ArrayList<Double>();
							data.add((double)i);
							data.add(score);
							int j = 0;
							for (; j < votelist.size(); j++) {
								if (votelist.get(j).get(1) < score) {
									break;
								}
							}
							votelist.add(j, data);
						}
					}
				}
				System.out.println("votelist: "+votelist);
				c = votelist.get(0).get(0).intValue();
				if (alive_bodyguard) {
					int tmp = c;
					for (int i = 0; i < votelist.size(); i++){
						if (guarded[c]) {
							c = votelist.get(i).get(0).intValue();
						}
						else break;
						if (i == votelist.size()) {
							c = tmp;
							alive_bodyguard = false;
						}
					}
				}
				if (!isValidIdx(c)) {
					return Talk.SKIP;
				}
				whisper_called = c;
				return (new Content(new AttackContentBuilder(currentGameInfo.getAgentList().get(c)))).getText();
			}
		}
		else {
			if (!co_whisper) {
				co_whisper = true;
				if (seer) {
					return (new Content(new ComingoutContentBuilder(me, Role.SEER))).getText();
				}
				if (medium) {
					return (new Content(new ComingoutContentBuilder(me, Role.MEDIUM))).getText();
				}
			}
		}
		return Talk.SKIP;
	}
	
	protected Agent chooseVote() {
		
		gamedata.add(new GameData(DataType.VOTESTART, day, meint, meint, false));
		sh.process(params, gamedata);
		sh2.process(params, gamedata);
		double mn = -1;
		int c = 0;
		
		if (currentGameInfo.getAliveAgentList().size() <= 3) {
			votecnt++;
			if (votecnt == 1) {
				for (int i = 0; i < numAgents; i++) {
					if (i != meint) {
						if (sh.gamestate.agents[i].Alive) {
							double score = 1 - getPred(i, Util.POSSESSED);
							if (mn < score) {
								mn = score;
								c = i;
							}
						}
					}
				}
			}
			else {
				c = -1;
				System.out.println("PP");
				for (int i = 0; i < numAgents; i++) {
					if (i != meint) {
						if (sh.gamestate.agents[i].Alive) {
							if (sh.gamestate.agents[i].votefor == meint) {
								c = i;
							}
						}
					}
				}
				if (c == -1) {
					for (int i = 0; i < numAgents; i++) {
						if (i != meint) {
							if (sh.gamestate.agents[i].Alive) {
								double score = getPred(i, Util.POSSESSED);
								if (mn < score) {
									mn = score;
									c = i;
								}
							}
						}
					}
				}
			}
		}
		else {
			if (numAgents == 5) {
				c = -1;
				mn = -1;
				for (int i = 0; i < numAgents; i++) {
					if (i != meint) {
						if (sh.gamestate.agents[i].Alive) {
							double score = sh.gamestate.cnt_vote(i) + getPred(i, Util.WEREWOLF);
							if (mn < score) {
								mn = score;
								c = i;
							}
						}
					}
				}
				if (c == -1) {
					mn = -1;
					for (int i = 0; i < numAgents; i++) {
						if (i != meint) {
							if (sh.gamestate.agents[i].Alive) {
								double score = getPred(i, Util.WEREWOLF);
								if (mn < score) {
									mn = score;
									c = i;
								}
							}
						}
					}
				}
			}
			else {
				for (int i = 0; i < numAgents; i++) {
					if (i != meint) {
						if (sh.gamestate.agents[i].Alive) {
							double score = sh.gamestate.cnt_vote(i);
							if (mn < score) {
								mn = score;
								c = i;
							}
						}
					}
				}
				if (mn * 2 < currentGameInfo.getAliveAgentList().size()) {
					if ((day < 3) && (!nakama[c])) {
						int tmp = (int)mn;
						mn = -1;
						for (int i = 0; i < numAgents; i++) {
							if (i != meint) {
								if (sh.gamestate.agents[i].Alive && nakama[i]) {
									double score = getPred(i, Util.WEREWOLF);
									if ((mn < score) && sh.gamestate.cnt_vote(i) + 1 < tmp) {
										mn = score;
										c = i;
									}
								}
							}
						}
					}
					else {
						mn = -1;
						for (int i = 0; i < numAgents; i++) {
							if (i != meint) {
								if (sh.gamestate.agents[i].Alive) {
									double score = getPred(i, Util.WEREWOLF);
									if (mn < score) {
										mn = score;
										c = i;
									}
								}
							}
						}
					}
				}
			}
		}

		if (!isValidIdx(c)) {
			return null;
		}
		else {
		return currentGameInfo.getAgentList().get(c);
		}
		
	}

	protected String chooseTalk() {
		
		if (lastTurn == -1 || (lastTalkTurn == lastTurn)) {
			gamedata.add(new GameData(DataType.TURNSTART, day, meint, meint, false));
			lastTurn++;
		}

		sh.process(params, gamedata);
		sh2.process(params, gamedata);
		lastTalkTurn = lastTurn;
		updateState(sh);
		updateState(sh2);

		if (update_sh) {
			System.out.println("SEARCH");
			update_sh = false;
			sh.search(1000);
			sh2.search(1000);
		}
		
		double mn = -1;
		int c = 0;

		if (seer) {
			if (!check_nakamaseer) {
				check_nakamaseer = true;
				boolean nakamaseer = false;
				int num_seer = 0;
				for (int i = 0; i < numAgents; i++) {
					if (i != meint) {
						if (sh.gamestate.agents[i].Alive && (agents[i].COrole == Role.SEER)) {
							num_seer += 1;
							if (nakama[i]) {
								nakamaseer = true;
							}
						}
					}
				}
				if ((num_seer >= 2) || nakamaseer) {
					seer = false;
					lastTalkTurn = -1;
					return chooseTalk();
				}
			}
			if (!doCO) {
				doCO = true;
				bodyguard = false;
				medium = false;
				return (new Content(new ComingoutContentBuilder(me, Role.SEER))).getText();
			}
			if (!houkoku) {
				houkoku = true;
				for (int i = 0; i < numAgents; i++) {
					if (i != meint) {
						if (sh.gamestate.agents[i].Alive) {
							if (divined[i] == 0) {
								double score = getPred(i, Util.WEREWOLF);
								if (mn < score) {
									mn = score;
									c = i;
								}
							}
						}
					}
				}
				if ((black_count >= 2) || nakama[c]) {
					divined[c] = 1;
					sh2.scorematrix.divined(sh2.gamestate, meint, c, true);
					return (new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(c), Species.HUMAN))).getText();
				}
				else {
					divined[c] = -1;
					black_count++;
					sh2.scorematrix.divined(sh2.gamestate, meint, c, false);
					return (new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(c), Species.WEREWOLF))).getText();
				}
			}
		}
		else if (bodyguard || medium) {
			if (!doCO) {
				int mediumCount = 0;
				boolean nakamaBodyguard = false, nakamaMedium = false;
				for (int i = 0; i < numAgents; i++) {
					if (nakama[i]) {
						if (agents[i].COrole == Role.BODYGUARD) {
							nakamaBodyguard = true;
						}
						else if (agents[i].COrole == Role.MEDIUM) {
							nakamaMedium = true;
						}
					}
					else if (agents[i].COrole == Role.MEDIUM)
						mediumCount++;
				}
				if ((!nakamaMedium) && (day == 1) && (mediumCount <= 1)) {
					medium = true;
					bodyguard = false;
					doCO = true;
					return (new Content(new ComingoutContentBuilder(me, Role.MEDIUM))).getText();
				}
				if (!nakamaBodyguard) {
					medium = false;
					bodyguard = true;
					doCO = true;
					return (new Content(new ComingoutContentBuilder(me, Role.BODYGUARD))).getText();
				}
				medium = false;
				bodyguard = false;
				doCO = true; 
				return (new Content(new ComingoutContentBuilder(me, Role.VILLAGER))).getText();
			}
			if (bodyguard) {
				if((!houkoku) && (day >= 2)) {
					houkoku = true;
					int[] seers = {-1, -1}, mediums = {-1, -1};
					int seer_count = 0, medium_count = 0;
					for (int i = 0; i < numAgents; i++) {
						if (i != meint) {
							if (sh.gamestate.agents[i].Alive) {
								if (agents[i].COrole == Role.SEER) {
									seers[seer_count] = i;
									seer_count = 1;
								}
								else if (agents[i].COrole == Role.MEDIUM) {
									mediums[medium_count] = i;
									medium_count = 1;
								}
							}
						}
					}
					if (seer_count == 1) {
						if (seers[1] != -1) {
							if (getPred(seers[0], Util.POSSESSED) > getPred(seers[1],  Util.POSSESSED)) {
								c = seers[0];
							}
							else {
								c = seers[1];
							}
						}
						else c = seers[0];
					}
					else if (medium_count == 1) {
						if (mediums[1] != -1) {
							if (getPred(mediums[0], Util.POSSESSED) > getPred(mediums[1], Util.POSSESSED)) {
								c = mediums[0];
							}
							else {
								c = mediums[1];
							}
						}
						else c = mediums[0];
					}
					else {
						mn = -1;
						for (int i = 0; i < numAgents; i++) {
							if (i != meint) {
								if (sh.gamestate.agents[i].Alive) {
									if (nakama[i]) {
										c = i;
										break;
									}
									else {
										double score = getPred(i, Util.POSSESSED);
										if (mn < score) {
											mn = score;
											c = i;
										}
									}
								}
							}
						}
					}
					return (new Content(new GuardedAgentContentBuilder(currentGameInfo.getAgentList().get(c)))).getText();
				}
			}
			else {
				if (!houkoku) {
					houkoku = true;
					if (currentGameInfo.getExecutedAgent() != null) {
						Agent target = currentGameInfo.getExecutedAgent();
						c = target.getAgentIdx() - 1;
						if (agents[c].COrole == Role.SEER) {
							return (new Content(new IdentContentBuilder(target, Species.HUMAN))).getText();
						}
						else if (black_count <= 1) {
							black_count ++;
							return (new Content(new IdentContentBuilder(target, Species.WEREWOLF))).getText();
						}
						return (new Content(new IdentContentBuilder(target, Species.HUMAN))).getText();
					}
				}
			}
		}
		
		if (currentGameInfo.getAliveAgentList().size() <= 3) {
			if (!pos) {
				pos = true;
				if (numAgents == 5) {
					double all = 0;
					double alive = 0;
					for (int i = 0; i < numAgents; i++) {
						all += getPred(i, Util.POSSESSED);
						if (sh.gamestate.agents[i].Alive) {
							alive += getPred(i, Util.POSSESSED);
						}
					}
					if (alive > 0.5 * all) {
						kyoujin_ikiteru = true;
						return (new Content(new ComingoutContentBuilder(me, Role.WEREWOLF))).getText();
					}
				}
				else {
					double all = 0;
					double alive = 0;
					for (int i = 0; i < numAgents; i++) {
						all += getPred(i, Util.POSSESSED);
						if (sh.gamestate.agents[i].Alive) {
							alive += getPred(i, Util.POSSESSED);
						}
					}
					if (alive > 0.5 * all) {
						kyoujin_ikiteru = true;
						return (new Content(new ComingoutContentBuilder(me, Role.WEREWOLF))).getText();
					}
				}
			}
			else if (kyoujin_ikiteru) {
				if (numAgents == 0) {
					System.out.println("kyojin");
					mn = -1;
					for (int i = 0; i < numAgents; i++)
						if (i != meint)
							if (sh.gamestate.agents[i].Alive) {
								double score = 1 - getPred(i, Util.POSSESSED);
								if (mn < score) {
									mn = score;
									c = i;
								}
							}
					voteCandidate = currentGameInfo.getAgentList().get(c);
					ContentRand = Math.random();
					if (ContentRand <= 0.3) {
						return (new Content(new VoteContentBuilder(voteCandidate))).getText();
					}
					else if (ContentRand > 0.3 && ContentRand <= 0.7) {
						return (new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF))).getText();
					}
					else {
						return (new Content(new RequestContentBuilder(Content.ANY, new Content(new VoteContentBuilder(voteCandidate))))).getText();
					}
				}
			}
		}

		if (numAgents == 15) {
			if (sh.gamestate.cnt_vote(meint) >= co_count) {
				for (int i = 0; i < numAgents; i++) {
					System.out.print(i + " will vote " + sh.gamestate.agents[i].will_vote + " ");
				}
				if (!doCO) {
					bodyguard = true;
					medium = true;
					lastTalkTurn = -1;
					return chooseTalk();
				}
			}
		}

		if (numAgents == 5) {
			c = -1;
			mn = -1;
			for (int i = 0; i < numAgents; i++) {
				if (i != meint) {
					if (sh.gamestate.agents[i].Alive) {
						double score = getPred(i, Util.WEREWOLF);
						if (mn < score) {
							mn = score;
							c = i;
						}
					}
				}
			}
			if (c == -1) {
				mn = -1;
				for (int i = 0; i < numAgents; i++) {
					if (i != meint) {
						if (sh.gamestate.agents[i].Alive) {
							double score = getPred(i, Util.WEREWOLF);
							if (mn < score) {
								mn = score;
								c = i;
							}
						}
					}
				}
			}
		}
		else {
			if (seer || bodyguard || medium) {
				for (int i = 0; i < numAgents; i++) {
					if (i != meint) {
						if (sh.gamestate.agents[i].Alive) {
							if (seer) {
								if (agents[i].COrole == Role.SEER) {
									ArrayList<Double> data = new ArrayList<Double>();
									data.add((double)i);
									data.add(getPred(i, Util.SEER));
									int j = 0;
									for (; j < taikou.size(); j++) {
										if (taikou.get(j).get(1) < data.get(1)) {
											break;
										}
									}
									taikou.add(j, data);
								}
							}
							else if (bodyguard) {								
								if (agents[i].COrole == Role.BODYGUARD) {
									ArrayList<Double> data = new ArrayList<Double>();
									data.add((double)i);
									data.add(getPred(i, Util.BODYGUARD));
									int j = 0;
									for (; j < taikou.size(); j++) {
										if (taikou.get(j).get(1) < data.get(1)) {
											break;
										}
									}
									taikou.add(j, data);
								}
							}
							else {
								if (agents[i].COrole == Role.MEDIUM) {
									ArrayList<Double> data = new ArrayList<Double>();
									data.add((double)i);
									data.add(getPred(i, Util.MEDIUM));
									int j = 0;
									for (; j < taikou.size(); j++) {
										if (taikou.get(j).get(1) < data.get(1)) {
											break;
										}
									}
									taikou.add(j, data);
								}
							}
						}
					}
				}
				if (taikou.size() >= 1)
					c = taikou.get(0).get(0).intValue();
					System.out.println("--------taikou---------\n" + taikou);
			}
			if (taikou.size() == 0) {
				for (int i = 0; i < numAgents; i++) {
					if (i != meint) {
						if ((sh.gamestate.agents[i].Alive) && (divined[i] != 1)) {
							double score = sh.gamestate.cnt_vote(i);
							if (mn < score) {
								mn = score;
								c = i;
							}
						}
					}
				}
				if (mn * 2 < currentGameInfo.getAliveAgentList().size()) {
					mn = -1;
					for (int i = 0; i < numAgents; i++) {
						if (i != meint) {
							if (sh.gamestate.agents[i].Alive) {
								double score = getPred(i, Util.WEREWOLF);
								if (nakama[i])
									score -= 0.4;
								if (mn < score) {
									mn = score;
									c = i;
								}
							}
						}
					}
				}
			}
		}
		
		if (!isValidIdx(c)) {
			return Talk.SKIP;
		}
		else {
			voteCandidate = currentGameInfo.getAgentList().get(c);
			ContentRand = Math.random();
			if (ContentRand <= 0.3) {
				return (new Content(new VoteContentBuilder(voteCandidate))).getText();
			}
			else if (ContentRand > 0.3 && ContentRand <= 0.7) {
				return (new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF))).getText();
			}
			else {
				return (new Content(new RequestContentBuilder(Content.ANY, new Content(new VoteContentBuilder(voteCandidate))))).getText();
			}
		}

	}

	protected Agent attackVote() {
		
		sh.process(params, gamedata);
		sh.update();
		
		double mn = -1;
		
		int c = 0;
		
		ArrayList<ArrayList<Double>> votelist = new ArrayList<ArrayList<Double>>();
		
		if (numAgents == 5) {
			for (int i = 0; i < numAgents; i++) {
				if (i != meint) {
					if (sh.gamestate.agents[i].Alive) {
						double score = 1 - getPred(i, Util.POSSESSED);
						if (mn < score) {
							mn = score;
							c = i;
						}
					}
				}
			}
		}
		else {
			if (whisper_called != -1) {
				return currentGameInfo.getAgentList().get(whisper_called);
			}
			if (alive_bodyguard) {
				for (int i = 0; i < numAgents; i++) {
					if (i != meint && !nakama[i]) {
						if (agents[i].COrole == Role.BODYGUARD) {
							if (sh.gamestate.agents[i].Alive) {
								return currentGameInfo.getAgentList().get(i);
							}
							else {
								alive_bodyguard = false;
							}
						}
					}
				}
			}
			for (int i = 0; i < numAgents; i++) {
				if (i != meint && !nakama[i]) {
					if (sh.gamestate.agents[i].Alive) {
						double score = 1 - getPred(i, Util.POSSESSED);
						score += 0.2 * getPred(i, Util.SEER);
						score += 0.1 * getPred(i, Util.BODYGUARD);
						score += 0.1 * getPred(i, Util.MEDIUM);
						score += 3 * wincnt[i] / (gamecount + 0.01);
						ArrayList<Double> data = new ArrayList<Double>();
						data.add((double)i);
						data.add(score);
						int j = 0;
						for (; j < votelist.size(); j++) {
							if (votelist.get(j).get(1) < score) {
								break;
							}
						}
						votelist.add(j, data);
					}
				}
			}
			c = votelist.get(0).get(0).intValue();
		}
		
		if (numAgents == 15) {
			if (alive_bodyguard) {
				int tmp = c;
				for (int i = 0; i < votelist.size(); i++){
					if (guarded[c]) {
						c = votelist.get(i).get(0).intValue();
					}
					else break;
					if (i == votelist.size()) {
						c = tmp;
						alive_bodyguard = false;
					}
				}
			}
		}

		if (!isValidIdx(c)) {
			return null;
		}
		else {
			return currentGameInfo.getAgentList().get(c);
		}
		
	}

}
