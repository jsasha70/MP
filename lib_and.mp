func and:2:1 {
  if {
    if {
      1:1>
    } else {
      0:1>
    }
  } else {
      >>_ 0:1>
  }
}

func and:64:32 #out 32:b {
  def{ arg1:32 arg2:32 }

  >arg2:32 >arg1:32 .

  loop 32 {
    arg1:32> 0:1> >arg1:32
    arg2:32> 0:1> >arg2:32
    >and:2:1>
  }
}
