LiteLoader
----------

Important Setup Note
--------------------
MCP 9.10 does not include the latest MCPBot mappings, since developing mods without these updated
mappings is a massive nuisance LiteLoader is built using a recent snapshot of the MCP mappings.

The latest MCPTEST mappings can always be retrieved from [http://mcpbot.bspk.rs/testcsv/](http://mcpbot.bspk.rs/testcsv/) 

Getting the updated mappings
----------------------------

To get the updated mappings, perform these steps **before** decompiling minecraft for the first 
time. If you have already decompiled minecraft, run the MPC **cleanup** task first to remove the
invalid sources, or simply delete the *temp* and *src* directories from your MCP installation.

-   Using a text editor, open **/mcp910/conf/mcp.cfg**
-   Locate the setting **UpdateUrl** and change the domain to *dl.liteloader.com* as shown

    UpdateUrl         = http://dl.liteloader.com/files/mcprolling_{version}/

-   Save the file and run the **updatemcp** script, it should prompt you to download 3 files.
-   Type **yes** to accept the download. 
-   Run **decompile** as normal.