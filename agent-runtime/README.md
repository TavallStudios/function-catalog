# Tavall Agent Runtime

Provider-neutral runtime primitives for Tavall custom AI agents.

Custom-agent placement is intentionally not decided in this module. Tavall Cloud owns placement and must restrict custom agent/model execution to explicitly classified development nodes. Providers receive only an `AIFunctionCatalogView`, never the root Function Catalog.

The runtime revokes the provider's Function Catalog view and interrupts provider execution when a job times out. Providers that launch external processes must treat interruption as a hard cancellation signal and terminate their owned process tree before returning control to the runtime.
