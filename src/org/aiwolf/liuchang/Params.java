/**
 * 
 */
package org.aiwolf.liuchang;

/**
 * @author liuch
 *
 */
public class Params {

	double defo;
	double diff;
	double mn;
	double mx;
	double value;
	
	String name;
	
	boolean valid = true;

	Params(double initial, double _mn, double _mx, String _name, double _diff) {
		defo = initial;
		value = initial;
		mn = _mn;
		mx = _mx;
		name = _name;
		diff = _diff;
	}
	
}
