program controller;
{$PROCESSOR PIC16F877}
{$OPTIMIZE SPEED}
{$FREQUENCY 4 MHZ}
{$CONFIG BOREN = OFF, CP = OFF, CPD = OFF, FOSC = HS, LVP = OFF, PWRTE = ON, WDTE = OFF, WRT = ON, DEBUG = OFF}
{$INIT COMPARATORS, ANALOGS}          // komparator kikapcs, analog be kikapcs = minden I/O bemenet
{$DEFINE BAUD_RATE 9600}
uses serial;

var
  ch1,ch2,ch3,ch4: char;
  t : array [0..9] of byte;
procedure initt;
begin
  t[0]:=b'00111111';
  t[1]:=b'00000110';
  t[2]:=b'01011011';
  t[3]:=b'01001111';
  t[4]:=b'01100110';
  t[5]:=b'01101101';
  t[6]:=b'01111101';
  t[7]:=b'00000111';
  t[8]:=b'01111111';
  t[9]:=b'01100111';
end;
procedure setA(num:byte);
begin
  PORTA.RA0:=(num and 64)=64;
  PORTA.RA1:=(num and 32)=32;
  PORTA.RA2:=(num and 16)=16;
  PORTA.RA3:=(num and 8)=8;
  PORTA.RA5:=(num and 4)=4;
  PORTE.RE0:=(num and 2)=2;
  PORTE.RE1:=(num and 1)=1;
end;
procedure setB(num:byte);
begin
  PORTC.RC0:=(num and 64)=64;
  PORTC.RC1:=(num and 32)=32;
  PORTC.RC2:=(num and 16)=16;
  PORTC.RC3:=(num and 8)=8;
  PORTD.RD0:=(num and 4)=4;
  PORTD.RD1:=(num and 2)=2;
  PORTD.RD2:=(num and 1)=1;
end;
procedure setC(num:byte);
begin
  PORTD.RD7:=(num and 64)=64;
  PORTD.RD6:=(num and 32)=32;
  PORTD.RD5:=(num and 16)=16;
  PORTD.RD4:=(num and 8)=8;
  PORTC.RC5:=(num and 4)=4;
  PORTC.RC4:=(num and 2)=2;
  PORTD.RD3:=(num and 1)=1;
end;
procedure setD(num:byte);
begin
  PORTB:=num*2;
end;
procedure setData(num1,num2,num3,num4:byte);
begin
  setA(num1);
  setB(num2);
  setC(num3);
  setD(num4);
end;
begin
  BAUD(9600,RX);
  ASSIGN(input, SerialPort_Input);
  ADCON1:=06;
  TRISB:=0;
  initt;
  loop
    Read(ch1);read(ch2);read(ch3);read(ch4);
    setData(t[ord(ch1)-48],t[ord(ch2)-48],t[ord(ch3)-48],t[ord(ch4)-48]);
  end;
end.