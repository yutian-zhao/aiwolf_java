/**
 * 
 */
package org.aiwolf.liuchang;

import org.aiwolf.common.data.Role;

/**
 * @author liuch
 *
 */
public class AgentInfo {

	int index;
	int NvotedBy;
	int state;
	int voteFor;
	int wincnt;
	
	boolean alive;
	
	Role role;
	Role COrole = null;
	
	AgentInfo() {
		state = -1;
		alive = true;
		wincnt = 0;
	}
	
}
