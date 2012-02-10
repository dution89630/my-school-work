package org.deuce.optimize.main.settings;

import java.util.EnumSet;

public enum OptimizerSettings {
	L00None, // 	
	L10Opt, // should be included if any of the following is included
	L11Imm, //
	L12Nic, //
	L13Nl, //
	L14Tl, //
	L15Rom, //
	L19Known, //
	L20Loops, //
	L30Ro, // 
	L40Wo, //
	L50Ip, //
	L60Cp, // 
	L70PlusStables, //

	//

//	IncludingLFA, //
	;

	// singletons
	public static EnumSet<OptimizerSettings> JustNone = EnumSet.of(L00None);
	public static EnumSet<OptimizerSettings> JustOpt = EnumSet.of(L10Opt);
	public static EnumSet<OptimizerSettings> JustImm = EnumSet.of(L10Opt,
			L11Imm);
	public static EnumSet<OptimizerSettings> JustNic = EnumSet.of(L10Opt,
			L12Nic);
	public static EnumSet<OptimizerSettings> JustNl = EnumSet.of(L10Opt, L13Nl);
	public static EnumSet<OptimizerSettings> JustNew = EnumSet.of(L10Opt,
			L12Nic, L13Nl);
	public static EnumSet<OptimizerSettings> JustTl = EnumSet.of(L10Opt, L14Tl);
	public static EnumSet<OptimizerSettings> JustRom = EnumSet.of(L10Opt,
			L15Rom);
	public static EnumSet<OptimizerSettings> JustLoops = EnumSet.of(L10Opt,
			L20Loops);
	public static EnumSet<OptimizerSettings> JustRo = EnumSet.of(L10Opt, L30Ro);
	public static EnumSet<OptimizerSettings> JustWo = EnumSet.of(L10Opt, L40Wo);
	public static EnumSet<OptimizerSettings> JustRoWo = EnumSet.of(L10Opt, L30Ro, L40Wo);
	public static EnumSet<OptimizerSettings> JustRes = EnumSet.of(L10Opt,
			L11Imm, L12Nic, L13Nl, L50Ip, L60Cp);

	// cumulatives
	public static EnumSet<OptimizerSettings> CumImmNic = EnumSet.of(L10Opt,
			L11Imm, L12Nic);
	public static EnumSet<OptimizerSettings> CumImmNicNl = EnumSet.of(L10Opt,
			L11Imm, L12Nic, L13Nl);
	public static EnumSet<OptimizerSettings> CumImmNicNlTl = EnumSet.of(L10Opt,
			L11Imm, L12Nic, L13Nl, L14Tl);
	public static EnumSet<OptimizerSettings> CumImmNicNlTlRom = EnumSet.of(
			L10Opt, L11Imm, L12Nic, L13Nl, L14Tl, L15Rom);
	public static EnumSet<OptimizerSettings> CumImmNicNlTlRomLoops = EnumSet
			.of(L10Opt, L11Imm, L12Nic, L13Nl, L14Tl, L15Rom, L20Loops);
	public static EnumSet<OptimizerSettings> CumImmNicNlTlRomLoopsRo = EnumSet
			.of(L10Opt, L11Imm, L12Nic, L13Nl, L14Tl, L15Rom, L20Loops, L30Ro);
	public static EnumSet<OptimizerSettings> CumImmNicNlTlRomLoopsRoWo = EnumSet
			.of(L10Opt, L11Imm, L12Nic, L13Nl, L14Tl, L15Rom, L20Loops, L30Ro,
					L40Wo);
	public static EnumSet<OptimizerSettings> CumImmNicNlTlRomLoopsRoWoIp = EnumSet
			.of(L10Opt, L11Imm, L12Nic, L13Nl, L14Tl, L15Rom, L20Loops, L30Ro,
					L40Wo, L50Ip);
	public static EnumSet<OptimizerSettings> CumImmNicNlTlRomLoopsRoWoIpCp = EnumSet
			.of(L10Opt, L11Imm, L12Nic, L13Nl, L14Tl, L15Rom, L20Loops, L30Ro,
					L40Wo, L50Ip, L60Cp);

}
