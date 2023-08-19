/**
 * 
 */
package org.aiwolf.liuchang;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.IdentContentBuilder;
import org.aiwolf.client.lib.RequestContentBuilder;
import org.aiwolf.client.lib.VoteContentBuilder;

import java.util.ArrayList;

/**
 * @author liuch
 *
 */
public class HowlsMedium extends HowlsBasePlayer {

	int target;
	
	boolean black;
	boolean doCO = false;
	boolean f = true;
	boolean houkoku = true;
	boolean white;
	boolean update_sh = true;
	
	double ContentRand;
	
	Judge ident;
	Parameters params;
	
	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		if (f) {
			params = new Parameters(numAgents);
			sh = new StateHolder(numAgents);
			f = false;
		}
		doCO = false;
		houkoku = true;
		update_sh = true;
		sh.process(params, gamedata);
		gamedata.clear();
		sh.head = 0;
		ArrayList<Integer> fixed = new ArrayList<Integer>();
		fixed.add(meint);
		sh.game_init(fixed, meint, numAgents, Util.MEDIUM, params);
	}
	
	@Override
	public void dayStart() {
		super.dayStart();
		ident = currentGameInfo.getMediumResult();
		if (ident != null) {
			houkoku = false;
			gamedata.add(new GameData(DataType.ID, day, meint, ident.getTarget().getAgentIdx() - 1, ident.getResult() == Species.HUMAN));
			target = ident.getTarget().getAgentIdx() - 1;
			black = (ident.getResult() == Species.WEREWOLF);
		}
		sh.process(params, gamedata);
	}
	
	protected Agent chooseVote() {
		gamedata.add(new GameData(DataType.VOTESTART, day, meint, meint, false));
		sh.process(params, gamedata);
		int c = 0;
		c = chooseMostLikelyWerewolf();
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
		lastTalkTurn = lastTurn;
		
		updateState(sh);
		if (update_sh) {
			update_sh = false;
			sh.search(1000);
		}
		
		int c = 0;
		c = chooseMostLikelyExecuted(getAliveAgentsCount() * 0.5);
		if (c == -1) {
			c = chooseMostLikelyWerewolf();
		}
		
		ContentRand = Math.random();
		if (!doCO && ContentRand > 0.5) {
			doCO = true;
			return (new Content(new ComingoutContentBuilder(me, Role.MEDIUM))).getText();
		}
		
		if (!houkoku) {
			houkoku = true;
			return (new Content(new IdentContentBuilder(currentGameInfo.getAgentList().get(target), black ? Species.WEREWOLF : Species.HUMAN))).getText();
		}
		
		if (!isValidIdx(c)) {
			return null;
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
	
}
