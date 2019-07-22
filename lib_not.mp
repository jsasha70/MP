func not:1:1 {
  if {
    0:1>
  } else {
    1:1>
  }
}

func not:32:32 #out 32:b {
  def{ arg:32 }

  >arg:32 .

  loop 32 {
    arg:32> 0:1> >arg:32
    >not:1:1>
  }
}
