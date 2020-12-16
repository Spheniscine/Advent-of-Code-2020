package commons

import java.util.Collections
import java.util.IdentityHashMap

class IdentityHashSet<T>: MutableSet<T> by Collections.newSetFromMap(IdentityHashMap())