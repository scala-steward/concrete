package cspfj.constraint;

import cspfj.exception.MatrixTooBigException;
import cspfj.problem.Variable;

public class MatrixManagerList extends AbstractMatrixManager {

    private final int[][][] listOfSupports ;

    private boolean supports ;
    
    public MatrixManagerList(Variable[] scope, int[] tuple, int[][][] last) {
        super(scope, tuple, last);
        this.listOfSupports= new int[arity][][];

    }

    @Override
    public void init(final boolean initialState) throws MatrixTooBigException {
        supports = !initialState ;
    }
    
    @Override
    public boolean set(final int[] tuple, final boolean status) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void intersect(final Variable[] scope, final Variable[] constraintScope,
            final boolean supports, final int[][] tuples) throws MatrixTooBigException {
        
        // TODO Auto-generated method stub

    }

    @Override
    public boolean setFirstTuple(final int variablePosition, final int index) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean next() {
        // TODO Auto-generated method stub
        return false;
    }

	@Override
	public boolean isTrue(int[] tuple) {
		// TODO Auto-generated method stub
		return false;
	}

}
