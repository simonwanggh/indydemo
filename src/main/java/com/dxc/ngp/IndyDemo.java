package com.dxc.ngp;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.shell.ParameterResolver;
import org.springframework.shell.jcommander.JCommanderParameterResolver;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.util.StringUtils;


@SpringBootApplication
@ComponentScan(basePackages = {"com.dxc.ngp"})
public class IndyDemo {
    public static void main(String[] args) {
    	String[] disabledCommands = {"--spring.shell.command.quit.enabled=false"}; 
        String[] fullArgs = StringUtils.concatenateStringArrays(args, disabledCommands);
        SpringApplication.run(IndyDemo.class, fullArgs);        
    }

    @Bean
    public PromptProvider myPromptProvider() {
        return () -> new AttributedString("Indy Demo:>",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
    }
    

}