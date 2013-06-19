package com.bazaarvoice.jolt.cardinality;

import com.bazaarvoice.jolt.common.WalkedPath;
import com.bazaarvoice.jolt.common.pathelement.AtPathElement;
import com.bazaarvoice.jolt.common.pathelement.LiteralPathElement;
import com.bazaarvoice.jolt.common.pathelement.MatchablePathElement;
import com.bazaarvoice.jolt.common.pathelement.PathElement;
import com.bazaarvoice.jolt.common.pathelement.StarAllPathElement;
import com.bazaarvoice.jolt.common.pathelement.StarRegexPathElement;
import com.bazaarvoice.jolt.common.pathelement.StarSinglePathElement;
import com.bazaarvoice.jolt.exception.SpecException;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * A Spec Object represents a single line from the JSON Cardinality Spec.
 *
 * At a minimum a single Spec has :
 *   Raw LHS spec value
 *   Some kind of PathElement (based off that raw LHS value)
 *
 * Additionally there are 2 distinct subclasses of the base Spec
 *  CardinalityLeafSpec : where the RHS is either "ONE" or "MANY"
 *  CardinalityCompositeSpec : where the RHS is a map of children Specs
 *
 * The tree structure of formed by the CompositeSpecs is what is used during the transform
 *  to do the parallel tree walk with the input data tree.
 *
 * During the parallel tree walk, a Path<Literal PathElements> is maintained, and used when
 *  a tree walk encounters a leaf spec.
 */
public abstract class CardinalitySpec {

    // The processed key from the Json config
    protected final MatchablePathElement pathElement;

    public CardinalitySpec( String rawJsonKey ) {
        List<PathElement> pathElements = parse( rawJsonKey );

        if ( pathElements.size() != 1 ){
            throw new SpecException( "CardinalityTransform invalid LHS:" + rawJsonKey + " can not contain '.'" );
        }

        PathElement pe =  pathElements.get( 0 );
        if ( ! ( pe instanceof MatchablePathElement ) ) {
            throw new SpecException( "Spec LHS key=" + rawJsonKey + " is not a valid LHS key." );
        }

        this.pathElement = (MatchablePathElement) pe;
    }

    //  once all the cardinalitytransform specific logic is extracted.
    public static List<PathElement> parse( String key )  {

        if ( key.contains("@") ) {
            return Arrays.<PathElement>asList( new AtPathElement( key ) );
        }
        else if ( "*".equals( key ) ) {
            return Arrays.<PathElement>asList( new StarAllPathElement( key ) );
        }
        else if ( key.contains( "*" ) ) {
            if ( StringUtils.countMatches( key, "*" ) == 1 ) {
                return Arrays.<PathElement>asList( new StarSinglePathElement( key ) );
            }
            else {
                return Arrays.<PathElement>asList( new StarRegexPathElement( key ) );
            }
        }
        else {
            return Arrays.<PathElement>asList( new LiteralPathElement( key ) );
        }
    }

    /**
     * This is the main recursive method of the CardinalityTransform parallel "spec" and "input" tree walk.
     *
     * It should return true if this Spec object was able to successfully apply itself given the
     *  inputKey and input object.
     *
     * In the context of the CardinalityTransform parallel treewalk, if this method returns a non-null Object,
     * the assumption is that no other sibling Cardinality specs need to look at this particular input key.
     *
     * @return true if this this spec "handles" the inputkey such that no sibling specs need to see it
     */
    public abstract boolean apply( String inputKey, Object input, WalkedPath walkedPath, Object parentContainer );
}