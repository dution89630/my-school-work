/* =============================================================================
 *
 * platform.h
 *
 * Platform-specific bindings
 *
 * =============================================================================
 */


#ifndef PLATFORM_H
#define PLATFORM_H 1


#if defined(SPARC) || defined(__sparc__)
#  include <stm/platform_sparc.h>
#else /* !SPARC (i.e., x86) */
#  include <stm/platform_x86.h>
#endif


#define CAS(m,c,s)  cas((intptr_t)(s),(intptr_t)(c),(intptr_t*)(m))

typedef unsigned long long TL2_TIMER_T;


#endif /* PLATFORM_H */


/* =============================================================================
 *
 * End of platform.h
 *
 * =============================================================================
 */
