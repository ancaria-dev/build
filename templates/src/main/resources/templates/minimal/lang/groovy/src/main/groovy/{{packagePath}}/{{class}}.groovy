package {{package}}

import dev.ancaria.coderpack.api.Context
import dev.ancaria.coderpack.api.SacredMod
import groovy.transform.CompileStatic

/** {{description}} */
// Static compilation: the same dispatch Java gets, and a typo is a compile
// error rather than a MissingMethodException mid-game. Drop the annotation on a
// class that wants Groovy's dynamic half.
@CompileStatic
class {{class}} implements SacredMod {

    @Override
    void onLoad(Context context) {
        context.log('Loaded.')
    }
}
