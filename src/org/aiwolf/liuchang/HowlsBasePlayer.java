/**
 * 
 */
package org.aiwolf.liuchang;

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

import org.aiwolf.client.lib.Content;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

// yutian
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;


/**
 * @author liuch
 *
 */
public class HowlsBasePlayer implements Player {
	// yutian
	StatusMatrix sm;
	static OrtEnvironment env = OrtEnvironment.getEnvironment();
	static OrtSession session;
	// static Logger logger = Logger.getLogger("MyLog");  
	// static FileHandler fh;
	// static SimpleFormatter formatter = new SimpleFormatter();
	int votingDay;
	// boolean debugLog = false;
	boolean debugPred = true; // whether print out debug information
	float[][] onnxPred; // record predicted probabilities for each role
	Map<String, Integer> roleStringInt = Map.of(
        "VILLAGER", 1,
        "SEER", 2,
        "MEDIUM", 3,
        "BODYGUARD", 4,
        "WEREWOLF", 5,
        "POSSESSED", 6
    );

	static int[] agentcnt;
	static int[][][] agentkoudou;
	static int gamenum;
	static int gamecount = 0;
	static int N_af = 11;
	static int pred = 0;
	static int rs;
	static int wincnt[];
	static int wincntbyrole[][];
	
	int day;
	int lastTalkTurn;
	int lastTurn;
	int max_day = 10;
	int max_turn = 5;
	int meint;
	int numAgents;
	int talkListHead;
	
	static boolean first = true;
	static boolean[] executedChecked;
	
	static double[][][][][] af;
	static double[][][][] agentScore;
	static double seerExecuted;
	
	Agent me;
	Agent voteCandidate;
	AgentInfo agents[];
	GameInfo currentGameInfo;
	Random rnd = new Random();
	StateHolder sh;
	
	ArrayList<GameData> gamedata = new ArrayList<GameData>();
	Deque<Content> talkQueue = new LinkedList<>();
	Deque<Content> whisperQueue = new LinkedList<>();
	List<Agent> humans = new ArrayList<>();
	List<Agent> werewolves = new ArrayList<>();
	List<Judge> identList = new ArrayList<>();
	Map<Integer, Agent> intToAgent = new HashMap<Integer, Agent>();
	Map<Role, Integer> roleint = new TreeMap<Role, Integer>();
	
	protected boolean isValidIdx(int a) {
		return a >= 0 && a < numAgents;
	}
	
	protected void updateState(StateHolder _sh) {
		boolean condition = false;
		if (numAgents == 5) {
			condition = ((day == 1 || day == 2) && sh.gamestate.turn <= 3 && sh.gamestate.turn >= 2);
		}
		else {
			condition = (day < max_day && sh.gamestate.turn <= 4 && sh.gamestate.turn >= 2);
		}
		if (condition) {
			int tu = _sh.gamestate.turn - 2;
			for (int r = 0; r < rs; r++) {
				for (int i = 0; i < numAgents; i++)
					if (sh.gamestate.agents[i].Alive) {
						_sh.scorematrix.scores[i][r][i][r] += Util.nlog(agentScore[day][tu][i][r]);
					}
			}
			_sh.update();
		}
	}
	
	// yutian
	double getPred(int agent, int role) {
		if (onnxPred != null){
			role = roleStringInt.get(Util.role_int_to_string[role]);
			return (double) onnxPred[role][agent];
		} else {
			return 0;
		}
		
	}

	protected int chooseMostLikelyWerewolf() {
	//最も人狼らしいプレイヤー
		int c = -1;
		double mn = -1e9; //-10^9
		for (int i = 0; i < numAgents; i++)
			if (i != meint) //自分じゃないなら
				if (sh.gamestate.agents[i].Alive) { //生きているなら
					double score = getPred(i, Util.WEREWOLF);
					// + sh.gamestate.cnt_vote(i) * 0.0001; 
					//(stateholder -> roleprediction で推定した人狼の可能性) + (そのプレイヤーに投票宣言している人の数 × 0.0001)
					if (mn < score) { //最大値を取ってくる
						mn = score;
						c = i;
					}
				}
		return c;
	}

	protected int chooseMostLikelyExecuted(double th) { 
	//最も追放されそうなプレイヤー
		int c = -1;
		double mn = th;
		for (int i = 0; i < numAgents; i++)
			if (i != meint) //自分じゃないなら
				if (sh.gamestate.agents[i].Alive) { //生きているなら
					double score = (double) onnxPred[6][i];
					// sh.gamestate.cnt_vote(i) + getPred(i, Util.WEREWOLF);
					//(そのプレイヤーに投票宣言している人の数) + (人狼の可能性 ※0~1の間)
					if (mn < score) { //最大値を取ってくる
						mn = score;
						c = i;
					}
				}
		return c;
	}
	
	protected int getAliveAgentsCount() {
		return currentGameInfo.getAliveAgentList().size();
	}
	
	protected boolean isAlive(Agent agent) {
		return currentGameInfo.getStatusMap().get(agent) == Status.ALIVE;
	}
	
	protected boolean isHuman(Agent agent) {
		return humans.contains(agent);
	}
	
	protected boolean isWerewolf(Agent agent) {
		return werewolves.contains(agent);
	}
	
	protected <T> T randomSelect(List<T> list) {
		if (list.isEmpty()) {
			return null;
		}
		else {
			return list.get((int) (Math.random() * list.size()));
		}
	}
	
	private void addExecutedAgent(Agent executedAgent) {
		if (executedAgent != null) {
			gamedata.add(new GameData(DataType.EXECUTED, day, -1, executedAgent.getAgentIdx() - 1, false));
		}
	}
	
	private void addKilledAgent(Agent killedAgent) {
		if (killedAgent != null) {
			gamedata.add(new GameData(DataType.KILLED, day, -1, killedAgent.getAgentIdx() - 1, true));
		}
	}
	
	@Override
	public Agent attack() {
		// TODO Auto-generated method stub
		return attackVote();
	}

	@Override
	public void dayStart() {
		if (day != currentGameInfo.getDay()) {
			day = currentGameInfo.getDay();
			// yutian 
			// getLatestVoteList -> getVoteList
			List<Vote> votelist = currentGameInfo.getVoteList();
			for (Vote v : votelist) {
				gamedata.add(new GameData(DataType.VOTE, day, v.getAgent().getAgentIdx() - 1, v.getTarget().getAgentIdx() - 1, false));
			}
			if (day != 0) {
				gamedata.add(new GameData(DataType.DAYCHANGE, -1, -1, -1, false));
			}
			for (int i = 0; i < numAgents; i++) {
				agents[i].voteFor = -1;
				agents[i].NvotedBy = 0;
			}
			talkQueue.clear();
			whisperQueue.clear();
			voteCandidate = null;
			talkListHead = 0;
			lastTurn = -1;
			lastTalkTurn = -1;
			addExecutedAgent(currentGameInfo.getExecutedAgent());

			// yutian
			// for debug: print out the executed player on the previous day
			if (debugPred && currentGameInfo.getExecutedAgent() != null) {
				System.out.println("YUTIAN Executed before day " + currentGameInfo.getDay()+ " is " + currentGameInfo.getExecutedAgent().getAgentIdx() + "(need -1)");
			}

			if (!currentGameInfo.getLastDeadAgentList().isEmpty()) {
				addKilledAgent(currentGameInfo.getLastDeadAgentList().get(0));
			}
		}
	}

	@Override
	public Agent divine() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void finish() {
		if (gamenum == gamecount) {
			gamecount++;
			for (int i = 0; i < numAgents; i++) {
				agents[i].alive = false;
			}
			for (Agent a : currentGameInfo.getAliveAgentList()) {
				int id = a.getAgentIdx() - 1;
				agents[id].alive = true;
			}
			Map<Agent, Role> result = currentGameInfo.getRoleMap();
			boolean werewolfWins = false;
			for (Map.Entry<Agent, Role> entry : result.entrySet()) {
				agents[entry.getKey().getAgentIdx() - 1].role = entry.getValue();
				if (entry.getValue() == Role.WEREWOLF) {
					if (agents[entry.getKey().getAgentIdx() - 1].alive) {
						werewolfWins = true;
					}
				}
				int id = entry.getKey().getAgentIdx() - 1;
				for (int d = 0; d < max_day; d++) {
					for (int t = 0; t < max_turn; t++) {
						if (agentkoudou[d][t][id] >= 0) {
							af[d][t][id][agentkoudou[d][t][id]][roleint.get(entry.getValue())]++;
						}
					}
				}
			}
			for (Map.Entry<Agent, Role> entry : result.entrySet()) {
				int id = entry.getKey().getAgentIdx() - 1;
				int ro = roleint.get(entry.getValue());
				if ((ro == Util.POSSESSED || ro == Util.WEREWOLF) == werewolfWins) {
					wincnt[id]++;
					wincntbyrole[id][ro]++;
				}
			}
			// yutian
			// for debug: print out prediction matrix and game results.
			if (debugPred) {
				int[] trueRole = new int[numAgents];
				for (int i = 0; i < trueRole.length; i++) {
					trueRole[i] = sm.roleint.get(agents[i].role);
				}
				float[][][][][] sourceArray = new float[1][sm.matrixList.size()][sm.matrixList.get(0).length][sm.matrixList.get(0)[0].length][sm.matrixList.get(0)[0].length];
				for (int i = 0; i < sourceArray.length; i++){
					for (int j = 0; j < sourceArray[0].length; j++){
						for (int k = 0; k < sourceArray[0][0].length; k++){
							for (int m = 0; m < sourceArray[0][0][0].length; m++){
								for (int n = 0; n < sourceArray[0][0][0][0].length; n++){
									sourceArray[i][j][k][m][n] = sm.matrixList.get(j)[k][m][n];
								}
							}
						}
					}
				}
				System.out.println("YUTIAN status matrix: "+Arrays.deepToString(sourceArray));
				System.out.println("YUTIAN sm length: "+sm.matrixList.size());
				System.out.println("YUTIAN finish day: "+currentGameInfo.getDay());
				System.out.println("YUTIAN result: "+Arrays.toString(trueRole));
			}
		}
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "MyBasePlayer";
	}

	@Override
	public Agent guard() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {

		numAgents = gameInfo.getAgentList().size();
		// yutian
		sm = new StatusMatrix();
		// logger.fine("Game start");
		// logger.fine(gameInfo.getRole().toString());
		
		if (first) {
			first = false;
			new Util();
			if (numAgents == 5)
				rs = 4;
			else
				rs = 6;
			wincnt = new int[numAgents];
			wincntbyrole = new int[numAgents][rs];
			af = new double[max_day][max_turn][numAgents][N_af][rs];
			agentScore = new double[max_day][max_turn][numAgents][rs];
			agentkoudou = new int[max_day][max_turn][numAgents];
			agentcnt = new int[rs];
			for (int d = 0; d < max_day; d++) {
				for (int t = 0; t < max_turn; t++) {
					for (int i = 0; i < numAgents; i++) {
						for (int j = 0; j < N_af; j++) {
							for (int k = 0; k < rs; k++) {
								af[d][t][i][j][k] = 0.1;
							}
						}
					}
				}
			}

			// yutian    
			
			// try {
			// 	logger.setLevel(Level.FINE);
			// 	if (debugLog) {
			// 		fh = new FileHandler("debug.log");
			// 		logger.addHandler(fh);
			// 		// set to info to hide the log
			// 		fh.setFormatter(formatter); 
			// 	}
			// } catch (IOException e) {  
			// 	System.out.println(e);;  
			// }

			try {
				InputStream model_is = getClass().getResourceAsStream("CNNLSTM_0819191143.onnx");
				byte[] model_bytes = model_is.readAllBytes();
				session = env.createSession(model_bytes, new OrtSession.SessionOptions());
			} catch (Exception e) {
				System.out.println(e);;
			}

			if (numAgents == 5) {
				agentcnt[Util.VILLAGER] = 2;
				agentcnt[Util.SEER] = 1;
				agentcnt[Util.POSSESSED] = 1;
				agentcnt[Util.WEREWOLF] = 1;
				for (int i = 0; i < numAgents; i++) {
					af[1][0][i][2][Util.SEER] = 4;
					af[1][0][i][2][Util.POSSESSED] = 4;
				}
			}
			else {
				agentcnt[Util.VILLAGER] = 8;
				agentcnt[Util.SEER] = 1;
				agentcnt[Util.BODYGUARD] = 1;
				agentcnt[Util.MEDIUM] = 1;
				agentcnt[Util.POSSESSED] = 1;
				agentcnt[Util.WEREWOLF] = 3;
				for (int i = 0; i < numAgents; i++) {
					af[1][0][i][2][Util.SEER] = 4;
					af[1][0][i][2][Util.POSSESSED] = 4;
					af[1][0][i][3][Util.MEDIUM] = 4;
				}
			}
			seerExecuted = 0;
		}
		
		for (int d = 0; d < max_day; d++) {
			for (int t = 0; t < max_turn; t++) {
				for (int i = 0; i < numAgents; i++) {
					agentkoudou[d][t][i] = -1;
				}
			}
		}
		
		roleint.put(Role.WEREWOLF, 0);
		roleint.put(Role.VILLAGER, 1);
		roleint.put(Role.SEER, 2);
		roleint.put(Role.POSSESSED, 3);
		roleint.put(Role.MEDIUM, 4);
		roleint.put(Role.BODYGUARD, 5);
		gamenum = gamecount;
		day = -1;
		me = gameInfo.getAgent();
		meint = me.getAgentIdx() - 1;
		pred = -1;
		
		for (Agent a : gameInfo.getAgentList()) {
			intToAgent.put(a.getAgentIdx() - 1, a);
		}
		
		agents = new AgentInfo[numAgents];
		for (int i = 0; i < numAgents; i++) {
			agents[i] = new AgentInfo();
		}
		for (Agent a : gameInfo.getAgentList()) {
			int id = a.getAgentIdx() - 1;
			agents[id].alive = true;
			agents[id].index = id;
		}
		
		identList.clear();
		humans.clear();
		werewolves.clear();
		executedChecked = new boolean[numAgents];
		for (int i = 0; i < numAgents; i++) {
			executedChecked[i] = false;
		}
		
	}

	@Override
	public String talk() {
		return chooseTalk();
	}

	@Override
	public void update(GameInfo gameInfo) {

		currentGameInfo = gameInfo;

		// yutian
		try {
			if (currentGameInfo.getDay() > 0){
				sm.update(currentGameInfo);
			} 
		} catch (Exception e){
			System.out.println(e);
		}
		if (sm.matrixList.size()>0){
			onnxPred = predict(sm, env, session);
		}
		
		for (int i = 0; i < numAgents; i++) {
			agents[i].alive = false;
		}
		
		for (Agent a : gameInfo.getAliveAgentList()) {
			int id = a.getAgentIdx() - 1;
			agents[id].alive = true;
		}
		
		addExecutedAgent(currentGameInfo.getLatestExecutedAgent());
		if (currentGameInfo.getExecutedAgent() != null) {
			int idx = currentGameInfo.getExecutedAgent().getAgentIdx() - 1;
			if ((!executedChecked[idx]) && (agents[idx].COrole == Role.SEER)) {
				executedChecked[idx] = true;
				int COseer = 0;
				for (int i = 0; i < numAgents; i++) {
					if (agents[i].COrole == Role.SEER) {
						COseer++;
					}
				}
				seerExecuted += 1 / (double)COseer;
			}
		}
		
		for (int i = talkListHead; i < currentGameInfo.getTalkList().size(); i++) {
			Talk talk = currentGameInfo.getTalkList().get(i);
			Agent talker = talk.getAgent();
			int da = talk.getDay();
			int tu = talk.getTurn();
			int italker = talker.getAgentIdx() - 1;
			Content content = new Content(talk.getText());
			if (da < max_day && tu < max_turn) {
				agentkoudou[da][tu][italker] = 0;
			}
			if (lastTurn < tu) {
				lastTurn = tu;
				gamedata.add(new GameData(DataType.TURNSTART, day, meint, meint, false));
			}
			switch (content.getTopic()) {
			case COMINGOUT:
				if (roleint.containsKey(content.getRole())) {
					agents[italker].COrole = content.getRole();
					gamedata.add(new GameData(DataType.CO, day, italker, roleint.get(content.getRole()), false));
					if (content.getRole() == Role.VILLAGER) {
						if (da < max_day && tu < max_turn) {
							agentkoudou[da][tu][italker] = 1;
						}
					}
					else if (content.getRole() == Role.SEER) {
						if (da < max_day && tu < max_turn) {
							agentkoudou[da][tu][italker] = 2;
						}
					}
					else if (content.getRole() == Role.MEDIUM) {
						if (da < max_day && tu < max_turn) {
							agentkoudou[da][tu][italker] = 3;
						}
					}
					else {
						if (da < max_day && tu < max_turn) {
							agentkoudou[da][tu][italker] = 0;
						}
					}
				}
				break;
			case DIVINED:
				gamedata.add(new GameData(DataType.TALKDIVINED, day, italker, content.getTarget().getAgentIdx() - 1, content.getResult() == Species.HUMAN));
				if (da < max_day && tu < max_turn) {
					if (content.getResult() == Species.HUMAN) {
						agentkoudou[da][tu][italker] = 4;
					}
					else {
						agentkoudou[da][tu][italker] = 5;
					}
				}
				break;
			case IDENTIFIED:
				gamedata.add(new GameData(DataType.ID, day, italker, content.getTarget().getAgentIdx() - 1, content.getResult() == Species.HUMAN));
				identList.add(new Judge(day, talker, content.getTarget(), content.getResult()));
				if (da < max_day && tu < max_turn) {
					agentkoudou[da][tu][italker] = 6;
				}
				break;
			case VOTE:
				gamedata.add(new GameData(DataType.WILLVOTE, day, italker, content.getTarget().getAgentIdx() - 1, false));
				agents[italker].voteFor = content.getTarget().getAgentIdx() - 1;
				if (da < max_day && tu < max_turn) {
					agentkoudou[da][tu][italker] = 7;
				}
				break;
			case ESTIMATE:
				if (content.getRole() == Role.WEREWOLF) {
					gamedata.add(new GameData(DataType.WILLVOTE, day, italker, content.getTarget().getAgentIdx() - 1, false));
				}
				if (da < max_day && tu < max_turn) {
					agentkoudou[da][tu][italker] = 8;
				}
				break;
			case SKIP:
				if (da < max_day && tu < max_turn) {
					agentkoudou[da][tu][italker] = 9;
				}
				break;
			case OVER:
				if (da < max_day && tu < max_turn) {
					agentkoudou[da][tu][italker] = 10;
				}
				break;
			default:
				if (da < max_day && tu < max_turn) {
					agentkoudou[da][tu][italker] = 0;
				}
				break;
			}
			if (da < max_day && tu < max_turn) {
				double ssum = 0;
				for (int k = 0; k < rs; k++) {
					double sum = 0;
					for (int r = 0; r < N_af; r++) {
						sum += af[da][tu][italker][r][k];
					}
					agentScore[da][tu][italker][k] = af[da][tu][italker][agentkoudou[da][tu][italker]][k] / sum;
					ssum += agentScore[da][tu][italker][k];
				}
				for (int k = 0; k < rs; k++) {
					agentScore[da][tu][italker][k] /= ssum;
				}
			}
		}
		
		talkListHead = currentGameInfo.getTalkList().size();
		
	}

	// yutian
	public float[][] predict(StatusMatrix sm, OrtEnvironment env, OrtSession session){
		float[][][][][] sourceArray = new float[1][sm.matrixList.size()][sm.matrixList.get(0).length][sm.matrixList.get(0)[0].length][sm.matrixList.get(0)[0].length];
		for (int i = 0; i < sourceArray.length; i++){
			for (int j = 0; j < sourceArray[0].length; j++){
				for (int k = 0; k < sourceArray[0][0].length; k++){
					for (int m = 0; m < sourceArray[0][0][0].length; m++){
						for (int n = 0; n < sourceArray[0][0][0][0].length; n++){
							sourceArray[i][j][k][m][n] = sm.matrixList.get(j)[k][m][n];
						}
					}
				}
			}
		}
		// logger.fine("day: " + sm.day);
		// logger.fine("len: " + sourceArray[0].length);
		// logger.fine(Arrays.deepToString(sourceArray[0][sourceArray[0].length-1]));
		OnnxTensor tensorFromArray;
		//TODO: 6 roles + aux
		float[][] pred = new float[7][sm.matrixList.get(0)[0].length];
		// float[] aux_pred = new float[sm.matrixList.get(0)[0].length];
		try {
			tensorFromArray = OnnxTensor.createTensor(env,sourceArray);
			Map<String, OnnxTensor> inputs = Map.of("modelInput", tensorFromArray);
			var results = session.run(inputs);
			float[][][][] res = (float[][][][]) results.get(2).getValue(); // B,C,L,D
			float[][][] aux_res = (float[][][]) results.get(1).getValue();
			for (int i=0; i<6; i++){
				pred[i] = res[0][res[0].length-1][i];
			}
			pred[6] = aux_res[0][res[0].length-1];
			// aux_pred = aux_res[0][res[0].length-1];
            // logger.fine(Arrays.deepToString(pred));
			// logger.fine(Arrays.toString(aux_pred));
		} catch (OrtException e) {
			System.out.println(e);
		} catch (Exception e){
			System.out.println(e);
		}
		// the result on the current day
		return pred;
	}

	protected Agent chooseVote() {
		return null;
	}

	protected String chooseTalk() {
		return null;
	}

	protected Agent attackVote() {
		return null;
	}

	@Override
	public Agent vote() {
		// yutian
		// records the first vote list if revote happens
		if (votingDay != 0 && votingDay == currentGameInfo.getDay()) {
			// latest votelist exits only when revote happens
			sm.preFirstVoteList = currentGameInfo.getLatestVoteList();
		} else {
			votingDay = currentGameInfo.getDay();
			sm.preFirstVoteList = new ArrayList<Vote>();
		}

		if (debugPred) {
			System.out.println("YUTIAN Vote on day " + currentGameInfo.getDay());
			System.out.println("YUTIAN onnxPred " + Arrays.deepToString(onnxPred));
			double[][] rp_prob;
			if (numAgents==5){
				rp_prob = new double[4][numAgents];
			} else {
				rp_prob = new double[6][numAgents];
			}
			for (int i = 0; i < rp_prob.length; i++){
				for (int j = 0; j < rp_prob[0].length; j++){
					rp_prob[i][j] = sh.rp.getProb(j, i); // note that order differs
				}
			}
			System.out.println("YUTIAN getProb " + Arrays.deepToString(rp_prob));
		}


		return chooseVote();
	}

	@Override
	public String whisper() {
		// TODO Auto-generated method stub
		return Talk.SKIP;
	}

}
