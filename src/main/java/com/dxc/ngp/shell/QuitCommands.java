package com.dxc.ngp.shell;

import java.util.concurrent.ExecutionException;

import org.hyperledger.indy.sdk.IndyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.ExitRequest;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.commands.Quit.Command;

import com.dxc.ngp.service.PoolLedgerService;
import com.dxc.ngp.service.WalletService;;

@ShellComponent
public class QuitCommands implements Command{
	@Autowired
	private PoolLedgerService poolService;
	
	@Autowired
	private WalletService walletService;
	
	@ShellMethod(value = "Exit the shell.", key = {"quit", "exit", "close"})
    public void quit() throws InterruptedException, ExecutionException, IndyException {
		try {
			if(poolService.hasOpenedPool()) {
				poolService.closeOpenedPool();
			}			
			walletService.closeAllWallet();
		}finally {
			throw new ExitRequest();
		}
    }

}
