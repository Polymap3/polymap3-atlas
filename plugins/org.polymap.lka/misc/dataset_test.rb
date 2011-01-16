# script to test adress data for integrity
#
# 2010 by Marcus -LiGi- Bueschleb for POLYMAP GmbH

require 'rio'

p "reading file"
content = rio('Mittelsachsen_adressen.txt').read

p "checking"

ok_cnt=0
sets=0
content.each_line { |l|

  splitted=l.split(";")

  # check coordinates
  if !(splitted[11].to_s =~ /(.*),.../)
    p "coordinate fail " +splitted[11]
  else 
    ok_cnt+=1
  end

  if !(splitted[12].to_s =~ /(.*),.../)
    p "coordinate fail " +splitted[12]
  else 
    ok_cnt+=1
  end


  # check if plz has correct length
  if (splitted[14].length!=5)
    p "plz fail " +splitted[14]
  else 
    ok_cnt+=1
  end


  # check if street filled
  if (splitted[13].length==0)
    p "street fail " +splitted[13]
  else 
    ok_cnt+=1
  end

  # check if city filled
  if (splitted[15].length==0)
    p "city fail " +splitted[15]
  else 
    ok_cnt+=1
  end


  sets+=1
}

p "tests passed with ok " + ok_cnt.to_s + " on " + sets.to_s + " datasets"
