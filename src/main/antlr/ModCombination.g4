grammar ModCombination;

MOD : 'NM' | 'EZ' | 'HT' | 'HR' | 'HD' | 'DT' | 'NC' | 'TD';
OR : '|';
LP : '(';
RP : ')';
LB : '{';
RB : '}';
NEG : '!';
COMMA : ',';

// a mod combination can be:
// * an exact restriction: 'HD', that means this beatmap must play with HD
// * an 'or' restriction: 'HD | HR', that means this beatmap must play with HD or HR
// * an 'and' restriction: 'HD HR', that means this beatmap must play with HD and HR
// * a grouped restriction: '(HD)', this is useful when combined with other restriction, such as 'HD(EZ|HR)',
//   that means this beatmap must play with HDEZ or HDHR
// * a combination restriction: '{ HD, HR }', that means this beatmap can only play with these mods or NM, such as NM, HD, HR or HDHR.
//   If you use '!{ HD, HR }', then 'NM' is not allowed.
expr : MOD
     | expr expr
     | expr OR expr
     | LP expr RP
     | NEG? LB expr (COMMA expr)* RB;

WS : ' ' -> skip;