select   ename, dname, job, empno, hiredate, loc
from     emp, dept
where    emp.deptno = dept.deptno
order by empno desc;