This module was introduced (at a very low "level", i.e., it would be available almost everywhere).
The purpose is to hold (in a more-or-less type-safe, but extremely lightweight way) constants that might be required across the stack, but which don't make sense to define at other abstraction layers.
It's possible that this will go away again if we find a better solution.