package commons

import java.util.Collections
import java.util.IdentityHashMap

class IdentityHashSet<T>: MapBackedSet<T>(IdentityHashMap())