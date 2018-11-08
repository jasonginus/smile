/*******************************************************************************
 * Copyright (c) 2010 Haifeng Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package smile.data.formula;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import smile.data.Tuple;

/**
 * The term of signum function.
 *
 * @author Haifeng Li
 */
public class Signum<T> implements Factor<T, Double> {
    /** The operand factor of signum expression. */
    private Factor<T, Double> child;

    /**
     * Constructor.
     *
     * @param factor the factor that signum function is applied to.
     */
    public Signum(Factor<T, Double> factor) {
        this.child = factor;
    }

    /**
     * Constructor.
     *
     * @param column the variable that signum function is applied to.
     */
    public Signum(String column) {
        this.child = new Column(column);
    }

    @Override
    public String name() {
        return String.format("signum(%s)", child.name());
    }

    @Override
    public List<Factor> factors() {
        return Collections.singletonList(this);
    }

    @Override
    public Set<String> variables() {
        return child.variables();
    }

    @Override
    public Double apply(T o) {
        return Math.signum(child.apply(o));
    }
}