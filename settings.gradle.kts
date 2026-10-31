rootProject.name = "aircheck-server"

// Core modules
include(":core:domain")
include(":core:service")
include(":core:airkorea-adapter")
include(":core:openmeteo-adapter")
include(":core:kma-adapter")
include(":core:persistence-adapter")
include(":core:bjd-matcher")
include(":core:fcm-adapter")
include(":core:firebase-adapter")

// Feature modules
include(":feature:weather")
include(":feature:admin")

// Util modules (헥사고날 계층과 무관한 기술적 유틸리티)
include(":util:kma-region")

// App (composition root)
include(":app")
