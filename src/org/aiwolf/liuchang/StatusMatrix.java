package org.aiwolf.liuchang;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.TalkType;
import org.aiwolf.client.lib.Topic;
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
    int day = -1;
    int talkListHead = 0;
    int[][][] dailyMatrix;
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

    static Content replaceSubject(Content content, Agent newSubject) {
		if (content.getTopic() == Topic.SKIP || content.getTopic() == Topic.OVER) {
			return content;
		}
		if (newSubject == Content.UNSPEC) {
			return new Content(Content.stripSubject(content.getText()));
		} else {
			return new Content(newSubject + " " + Content.stripSubject(content.getText()));
		}
	}

    public void update(GameInfo gameInfo) {
		currentGameInfo = gameInfo;
        Role myRole = currentGameInfo.getRole();
        
		// id, alive, comeout, vote, [specific], estimate, divine, identified, declare, [attitude]
        // daystart
        // vote
        // if (day == 0 && matrixList.size()==0){
        //     zeroData(dailyMatrix);
        //     matrixList.add(dailyMatrix);
        // }
        if (currentGameInfo.getDay() > day) {
            day = gameInfo.getDay();
            dailyMatrix = new int[channels][dim][dim];
            zeroData(dailyMatrix);
            matrixList.add(dailyMatrix);
            talkListHead = 0;
            // getLatestVoteList -> getVoteList
            // List<Vote> votelist = currentGameInfo.getVoteList();
            // this is the first vote result
            if (preFirstVoteList.size()==0){
                for (Vote v : currentGameInfo.getVoteList()) {
                    if (v.getAgent().getAgentIdx()>0 && v.getTarget().getAgentIdx()>0){
                        dailyMatrix[2][v.getAgent().getAgentIdx()-1][v.getTarget().getAgentIdx()-1] = 1;
                    }
                }
            } else {
                for (Vote v : preFirstVoteList) {
                    if (v.getAgent().getAgentIdx()>0 && v.getTarget().getAgentIdx()>0){
                        dailyMatrix[2][v.getAgent().getAgentIdx()-1][v.getTarget().getAgentIdx()-1] = 1;
                    } 
                }
                preFirstVoteList = new ArrayList<Vote>();
            }
			
            // id
            for (Map.Entry<Agent, Role> entry : currentGameInfo.getRoleMap().entrySet()){
                Arrays.fill(dailyMatrix[0][entry.getKey().getAgentIdx()-1], roleint.get(entry.getValue()));
            }

            // Skill
            Judge skillResult = null;
            if (myRole == Role.SEER){
                skillResult = currentGameInfo.getDivineResult();
            } else if (myRole == Role.MEDIUM){
                skillResult = currentGameInfo.getMediumResult();
            } 
            if (skillResult != null && skillResult.getTarget().getAgentIdx()>0) {
                if (skillResult.getResult() == Species.HUMAN){
                    Arrays.fill(dailyMatrix[3][skillResult.getTarget().getAgentIdx()-1], 1);
                } else {
                    Arrays.fill(dailyMatrix[3][skillResult.getTarget().getAgentIdx()-1], -1);
                }
            }
        }
        
		// alive (daystart and after voting)
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
			// int italker = talker.getAgentIdx() - 1;
			Content content = new Content(talk.getText());

            // subjectがUNSPECの場合は発話者に入れ替える
			if (content.getSubject() == Content.UNSPEC) {
				content = replaceSubject(content, talker);
			}

			try {
                parseSentence(content, talker, true);
            } catch (Exception e) {
                System.out.println("YUTIAN "+talk.getText());
                System.out.println(e);
            }

		}
		talkListHead = currentGameInfo.getTalkList().size(); //次にtalkを読むときの開始位置を設定
    }

    // 再帰的に文を解析する
	ArrayList<int[]> parseSentence(Content content, Agent talker, boolean update) {
        ArrayList<int[]> target_attitude = new ArrayList<int[]>();
        int italker = talker.getAgentIdx()-1;
        Agent target;
        Talk referedTalk;
        ArrayList<int[]> returned_target_attitude;
        Content referedContent;
		switch (content.getTopic()) {
        case ESTIMATE:
            if (content.getSubject() == talker){
                target = content.getTarget();
                if (target!=null && target.getAgentIdx()>0){
                    int itarget = target.getAgentIdx()-1;
                    Role estimateRole = content.getRole();
                    if (roleint.get(estimateRole)!=null){
                        int estimateResult = roleint.get(estimateRole);
                        int attitude = -1;
                        if (estimateRole.getSpecies()==Species.HUMAN){
                            attitude = 1;
                        }
                        int[] ta = new int[]{itarget, attitude};
                        target_attitude.add(ta);
                        if (update){
                            dailyMatrix[5][italker][itarget] = estimateResult;
                            dailyMatrix[6][italker][itarget] = attitude;
                        }
                    }
                }
            }
            break;
		case COMINGOUT:
            if (content.getSubject() == talker){
                Role coRole = content.getRole();
                if (roleint.get(coRole)!=null){
                    int coResult = roleint.get(coRole);
                    int[] ta = new int[]{italker, 1};
                    target_attitude.add(ta);
                    if (update){
                        Arrays.fill(dailyMatrix[4][italker], coResult);
                    }
                }
            }
            break;
		case DIVINATION:
            if (update){
                Arrays.fill(dailyMatrix[4][italker], roleint.get(Role.SEER));
            }
            target = content.getTarget();
            if (target!=null && target.getAgentIdx()>0){
                int itarget = target.getAgentIdx()-1;
                int[] ta = new int[]{itarget, -1};
                target_attitude.add(ta);
                if (update){
                    dailyMatrix[6][italker][itarget] = -1;
                }
            }
            break;
        case DIVINED:
            if (content.getSubject() == talker){
                if (update){
                    Arrays.fill(dailyMatrix[4][italker], roleint.get(Role.SEER));
                }
                target = content.getTarget();
                if (target!=null && target.getAgentIdx()>0){
                    int itarget = target.getAgentIdx()-1;
                    int divinedResult = 1; 
                    if (content.getResult() != Species.HUMAN){
                        divinedResult = -1;
                    }
                    int[] ta = new int[]{itarget, divinedResult};
                    target_attitude.add(ta);
                    if (update){
                        dailyMatrix[7][italker][itarget] = divinedResult;
                    }
                }
            }
            break;
		case IDENTIFIED:
            if (content.getSubject() == talker){
                if (update){
                    Arrays.fill(dailyMatrix[4][italker], roleint.get(Role.MEDIUM));
                }
                target = content.getTarget();
                if (target!=null && target.getAgentIdx()>0){
                    int itarget = target.getAgentIdx()-1;
                    int idfResult = 1; 
                    if (content.getResult() != Species.HUMAN){
                        idfResult = -1;
                    }
                    int[] ta = new int[]{itarget, idfResult};
                    target_attitude.add(ta);
                    if (update){
                        dailyMatrix[7][italker][itarget] = idfResult;
                    }
                }
            }
            break;
        case GUARD:
        case GUARDED:
            if (content.getSubject() == talker){
                if (update){
                    Arrays.fill(dailyMatrix[4][italker], roleint.get(Role.BODYGUARD));
                }
                target = content.getTarget();
                if (target!=null && target.getAgentIdx()>0){
                    int itarget = target.getAgentIdx()-1;
                    int[] ta = new int[]{itarget, 1};
                    target_attitude.add(ta);
                    if (update){
                        dailyMatrix[6][italker][itarget] = 1;
                    }
                }
            }
            break;
        case VOTE:
            target = content.getTarget();
            if (target!=null && target.getAgentIdx()>0){
                int itarget = target.getAgentIdx()-1;
                int[] ta = new int[]{itarget, -1};
                target_attitude.add(ta);
                if (update){
                    dailyMatrix[6][italker][itarget] = -1;
                }
            }
            break;
        case AGREE:
            if (content.getSubject() == talker){
                if (content.getTalkDay()==content.getDay() && content.getTalkType()==TalkType.TALK){
                    referedTalk = currentGameInfo.getTalkList().get(content.getTalkID());
                    Agent referedTalker = referedTalk.getAgent();
                    referedContent = new Content(referedTalk.getText());
                    if (referedContent.getSubject() == Content.UNSPEC) {
                        referedContent = replaceSubject(referedContent, referedTalker);
                    }
                    returned_target_attitude = parseSentence(referedContent, referedTalker, false);
                    for (int[] ta : returned_target_attitude){
                        target_attitude.add(ta);
                        if (update){
                            dailyMatrix[6][italker][ta[0]] = ta[1];
                        }
                    }
                } 
            }
            break;
        case DISAGREE:
            if (content.getSubject() == talker){
                if (content.getTalkDay()==content.getDay() && content.getTalkType()==TalkType.TALK){
                    referedTalk = currentGameInfo.getTalkList().get(content.getTalkID());
                    Agent referedTalker = referedTalk.getAgent();
                    referedContent = new Content(referedTalk.getText());
                    if (referedContent.getSubject() == Content.UNSPEC) {
                        referedContent = replaceSubject(referedContent, referedTalker);
                    }
                    returned_target_attitude = parseSentence(referedContent, referedTalker, false);
                    for (int[] ta : returned_target_attitude){
                        target_attitude.add(new int[]{ta[0], -ta[1]});
                        if (update){
                            dailyMatrix[6][italker][ta[0]] = -ta[1];
                        }
                    }
                } 
            }
            break;
		case OPERATOR:
            switch (content.getOperator()) {
            case REQUEST:
                referedContent = content.getContentList().get(0);
                if (referedContent.getSubject() == Content.UNSPEC) {
                    referedContent = replaceSubject(referedContent, talker);
                    returned_target_attitude = parseSentence(referedContent, talker, false);
                } else {
                    returned_target_attitude = parseSentence(referedContent, referedContent.getSubject(), false);
                }
                for (int[] ta : returned_target_attitude){
                    target_attitude.add(ta);
                    if (update){
                        dailyMatrix[6][italker][ta[0]] = ta[1];
                    }
                }
                break;
            case BECAUSE:
                referedContent = content.getContentList().get(1);
                if (referedContent.getSubject() == Content.UNSPEC) {
                    referedContent = replaceSubject(referedContent, talker);
                    returned_target_attitude = parseSentence(referedContent, talker, false);
                } else {
                    returned_target_attitude = parseSentence(referedContent, referedContent.getSubject(), true);
                }
                for (int[] ta : returned_target_attitude){
                    target_attitude.add(ta);
                    if (update){
                        dailyMatrix[6][italker][ta[0]] = ta[1];
                    }
                }
                break;
            case INQUIRE:
                break;
            case NOT:
                referedContent = content.getContentList().get(0);
                if (referedContent.getSubject() == Content.UNSPEC) {
                    referedContent = replaceSubject(referedContent, talker);
                    returned_target_attitude = parseSentence(referedContent, talker, false);
                } else {
                    returned_target_attitude = parseSentence(referedContent, referedContent.getSubject(), false);
                }
                for (int[] ta : returned_target_attitude){
                    target_attitude.add(new int[]{ta[0], -ta[1]});
                    if (update){
                        dailyMatrix[6][italker][ta[0]] = -ta[1];
                    }
                }
                break;
            default:
                for (Content c : content.getContentList()){
                    if (c.getSubject() == Content.UNSPEC) {
                        c = replaceSubject(c, talker);
                        returned_target_attitude = parseSentence(c, talker, true);
                    } else {
                        returned_target_attitude = parseSentence(c, c.getSubject(), true);
                    }
                    for (int[] ta : returned_target_attitude){
                        target_attitude.add(ta);
                        if (update){
                            dailyMatrix[6][italker][ta[0]] = ta[1];
                    }
                }
                }
                
                break;
            }
            break;
        default:
            break;
        }
        return target_attitude;
	}
    
}