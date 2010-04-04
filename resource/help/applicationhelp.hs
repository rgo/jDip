<?xml version='1.0' encoding='ISO-8859-1' ?>
<!DOCTYPE helpset
  PUBLIC "-//Sun Microsystems Inc.//DTD JavaHelp HelpSet Version 2.0//EN"
         "http://java.sun.com/products/javahelp/helpset_2_0.dtd">
<helpset version="2.0">
	<title>jDip Help</title>
	<maps>
		<homeID>Welcome</homeID>
		<mapref location="applicationhelp_map.jhm"/>
	</maps>

	<view>
		<name>TOC</name>
		<label>Table of Contents</label>
		<type>javax.help.TOCView</type>
		<data>applicationhelp_toc.xml</data>
	</view>
	
	<presentation default="true" displayviewimages="false">
		<name>main_help_window</name>
		<size width="640" height="480"/>
		<location x="150" y="150"/>
		<title>jDip Help</title>
		<image>cornericon</image>
		<toolbar>
			<helpaction>javax.help.BackAction</helpaction>
			<helpaction>javax.help.ForwardAction</helpaction>
			<helpaction>javax.help.SeparatorAction</helpaction>
			<helpaction>javax.help.HomeAction</helpaction>
			<helpaction>javax.help.SeparatorAction</helpaction>
			<helpaction>javax.help.PrintAction</helpaction>
			<helpaction>javax.help.PrintSetupAction</helpaction>
		</toolbar>
	</presentation>
</helpset>
