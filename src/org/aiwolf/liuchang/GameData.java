/**
 * 
 */
package org.aiwolf.liuchang;

/**
 * @author liuch
 *
 */
public class GameData {

	DataType type;
	
	int day;
	int object;
	int talker;
	
	boolean white;

	GameData(DataType _type, int _day, int _talker, int _object, boolean _white) {
		type = _type;
		day = _day;
		talker = _talker;
		object = _object;
		white = _white;
	}
	
}
