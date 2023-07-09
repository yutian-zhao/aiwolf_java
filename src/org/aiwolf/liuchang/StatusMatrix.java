package org.aiwolf.liuchang;

import org.aiwolf.client.lib.Content;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Status;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.data.Vote;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class StatusMatrix {
    int dim = 15;
    int channels = 8;
    // int?
    List<int[][][]> matrixList = new ArrayList<int[][][]>();
    int day = 0;
    int talkListHead = 0;
    int[][][] dailyMatrix = new int[channels][dim][dim];
    GameInfo currentGameInfo;
    List<Vote> preFirstVoteList = new ArrayList<Vote>();
    List<Role> roles = Arrays.asList(Role.VILLAGER, Role.SEER, Role.MEDIUM, Role.BODYGUARD, Role.WEREWOLF, Role.POSSESSED);
    Map<Role, Integer> roleint = Map.of(
        Role.VILLAGER, 1,
        Role.SEER, 2,
        Role.MEDIUM, 3,
        Role.BODYGUARD, 4,
        Role.WEREWOLF, 5,
        Role.POSSESSED, 6
    );

    public StatusMatrix() {
        super();
    }

    public StatusMatrix(int dim) {
        super();
        this.dim = dim;
    }

    public static void zeroData(int[][][] data) {
        // Zero the array
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                Arrays.fill(data[i][j], 0);
            }
        }
    }

    public void update(GameInfo gameInfo) {
		currentGameInfo = gameInfo;
        
		// id, alive, comeout, vote, [specific], estimate, divine, identified, declare, [attitude]
        // daystart
        // vote
        if (day == 0 && matrixList.size()==0){
            zeroData(dailyMatrix);
            matrixList.add(dailyMatrix);
        }
        if (currentGameInfo.getDay() > day) {
            // TODO: assert currentGameInfo.getDay() == day + 1
            day = gameInfo.getDay();
            dailyMatrix = new int[channels][dim][dim];
            zeroData(dailyMatrix);
            matrixList.add(dailyMatrix);
            talkListHead = 0;
            // getLatestVoteList -> getVoteList
            // List<Vote> votelist = currentGameInfo.getVoteList();
            // TODO: this is the second vote result
            if (preFirstVoteList.size()==0){
                for (Vote v : currentGameInfo.getVoteList()) {
                    dailyMatrix[3][v.getAgent().getAgentIdx()-1][v.getTarget().getAgentIdx()-1] = 1; 
                }
            } else {
                for (Vote v : preFirstVoteList) {
                    dailyMatrix[3][v.getAgent().getAgentIdx()-1][v.getTarget().getAgentIdx()-1] = 1; 
                }
                preFirstVoteList = new ArrayList<Vote>();
            }
			
            // id
            for (Map.Entry<Agent, Role> entry : currentGameInfo.getRoleMap().entrySet()){
                // TODO: Assert not none
                Arrays.fill(dailyMatrix[0][entry.getKey().getAgentIdx()-1], roleint.get(entry.getValue()));
            }
        }
        
		// alive
		for (int i = 0; i < dim; i++)
			Arrays.fill(dailyMatrix[1][i], 0);
		for (Agent a : currentGameInfo.getAliveAgentList()) {
			int id = a.getAgentIdx() - 1;
			Arrays.fill(dailyMatrix[1][id], 1);
		}

		//行動(発言)の解析
		for (int i = talkListHead; i < currentGameInfo.getTalkList().size(); i++) {
			Talk talk = currentGameInfo.getTalkList().get(i);
			Agent talker = talk.getAgent();
			int italker = talker.getAgentIdx() - 1;
			Content content = new Content(talk.getText());

			switch (content.getTopic()) {
			case COMINGOUT:
				if (roleint.containsKey(content.getRole())) {
					Arrays.fill(dailyMatrix[2][italker], roleint.get(content.getRole()));
				}
				break;
            case ESTIMATE:
                if (roleint.containsKey(content.getRole())) {
                    dailyMatrix[4][italker][content.getTarget().getAgentIdx() - 1] = roleint.get(content.getRole());
                }
				break;
			case DIVINED:
                int divineResult = -1;
                if (content.getResult() == Species.HUMAN){
                    divineResult = 1;
                }
                dailyMatrix[5][italker][content.getTarget().getAgentIdx() - 1] = divineResult;
				break;
			case IDENTIFIED:
                int identifyResult = -1;
                if (content.getResult() == Species.HUMAN){
                    identifyResult = 1;
                }
                dailyMatrix[6][italker][content.getTarget().getAgentIdx() - 1] = identifyResult;
                break;
			case VOTE:
                dailyMatrix[7][italker][content.getTarget().getAgentIdx() - 1] = 1;
                break;
            default:
                break;
			}

		}
		talkListHead = currentGameInfo.getTalkList().size(); //次にtalkを読むときの開始位置を設定
    }

}