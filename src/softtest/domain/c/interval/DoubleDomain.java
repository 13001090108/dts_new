package softtest.domain.c.interval;

import java.util.*;

import softtest.config.c.Config;



/** ������ */
public class DoubleDomain extends Domain {
	/** ���� */
	public static final DoubleDomain NULL = new DoubleDomain();
	public static double DEFAULT_MIN=Double.NEGATIVE_INFINITY,
	                       DEFAULT_MAX=Double.POSITIVE_INFINITY;

	@Override
	public Domain clone() throws CloneNotSupportedException {
		DoubleDomain domain=(DoubleDomain)super.clone();
		domain.intervals=new TreeSet<DoubleInterval>();
		for(DoubleInterval i:intervals){
			domain.intervals.add((DoubleInterval)i.clone());
		}
		return domain;
	}

	/** ���伯�ϣ���С���󱣳�˳��� */
	private TreeSet<DoubleInterval> intervals = new TreeSet<DoubleInterval>();
	
	/** �õ�һ��ȫ��*/
	public static DoubleDomain getFullDomain(){
		DoubleDomain r=new DoubleDomain(Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY);
		return r;
	}
	
	/** �õ�һ������*/
	public static DoubleDomain getEmptyDomain(){
		DoubleDomain r=new DoubleDomain(Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY);
		return r;
	}
	
	/** �õ����е����伯��*/
	public TreeSet<DoubleInterval> getIntervals(){
		return intervals;
	}

	/** ȱʡ���캯�� */
	public DoubleDomain() {
		domaintype=DomainType.DOUBLE;
	}

	/** ��ָ���ĵ�һ����[min,max]������minexcluded��maxexcludedָ��min��max�Ƿ�ȥ����Ϊtrue�Ļ���Ϊ�����䣩 */
	public DoubleDomain(double min, double max, boolean minexcluded, boolean maxexcluded) {
		this();
		
		if(Double.isNaN(min)){
			min=Double.NEGATIVE_INFINITY;	
		}
		if(Double.isNaN(max)){
			max=Double.POSITIVE_INFINITY;
		}
		DoubleInterval interval = new DoubleInterval(min, max, minexcluded, maxexcluded);
		
		if (!interval.isEmpty()) {
			intervals.add(interval);
		}
	}
	
	/** ��ָ���ĵ�һ����[min,max]������*/
	public DoubleDomain(double min, double max) {
		this(min,max,false,false);
	}

	/** �Զ�����乹���� */
	public DoubleDomain(DoubleInterval[] intervals) {
		this();
		for (int i = 0; i < intervals.length; i++) {
			this.mergeWith(intervals[i]);
		}
	}
	
	/**
	 * �õ�һ��������δ֪����
	 * 
	 * @return һ��������δ֪����
	 */
	public static DoubleDomain getUnknownDomain() {
		DoubleDomain r = new DoubleDomain();
		r.setUnknown(true);
		return r;
	}

	/** �Ե�һ���乹���� */
	public DoubleDomain(DoubleInterval interval) {
		this();
		this.mergeWith(interval);
	}

	/** ���������򣬿��������л�ȥ�����õĿ����� */
	public DoubleDomain(DoubleDomain domain) {
		this();
		for (DoubleInterval interval : domain.intervals) {
			if (!interval.isEmpty()) {
				this.intervals.add(new DoubleInterval(interval));
			}
		}
		this.setUnknown(domain.isUnknown());
	}

	/** �ж����Ƿ�Ϊ�� */
	public boolean isEmpty() {
		DoubleDomain d = new DoubleDomain(this);
		if (d.unknown) {
			return false;
		}
		if (d.intervals.isEmpty()) {
			return true;
		} else {
			return false;
		}
	}
	
	/** �жϵ�ǰ���Ƿ����������x*/
	public boolean contains(double x){
		if (unknown) {
			return true;
		}
		for (DoubleInterval interval : intervals) {
			if(interval.contains(x)){
				return true;
			}
		}
		return false;
	}
	
	/** �жϵ�ǰ���Ƿ������x*/
	public boolean contains(DoubleDomain x){
		if (unknown) {
			return true;
		}
		if (x.unknown && !unknown) {
			return false;
		}
		DoubleDomain temp=DoubleDomain.union(this, x);
		if(temp.equals(this)){
			return true;
		}
		return false;
	}

	/** �ж����Ƿ�ֵ����һ������ */
	@Override
	public boolean isCanonical() {
		if (unknown) {
			return false;
		}
		DoubleDomain d = new DoubleDomain(this);
		if (d.intervals.size() == 1) {
			for (DoubleInterval interval : d.intervals) {
				if (interval.isCanonical()) {
					return true;
				}
			}
		}
		return false;
	}

	/** �ж����Ƿ���� */
	@Override
	public boolean equals(Object o) {
		if ((o == null) || !(o instanceof DoubleDomain)) {
			return false;
		}
		if (this == o) {
			return true;
		}
		DoubleDomain x = (DoubleDomain) o;
		
		if (this.unknown && x.unknown) {
			return true;
		}
		
		if(isEmpty()&&x.isEmpty()){
			return true;
		}

		DoubleDomain a = new DoubleDomain(this), b = new DoubleDomain(x);
		if (a.intervals.size() == b.intervals.size()) {
			Iterator<DoubleInterval> ia = a.intervals.iterator(), ib = b.intervals.iterator();
			while (ia.hasNext()) {
				DoubleInterval interval1 = ia.next(), interval2 = ib.next();
				if (!interval1.equals(interval2)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/** ��ӡ */
	@Override
	public String toString() {
		StringBuffer b = new StringBuffer();
		if (unknown) {
			b.append("unknown");
		} else if (intervals.isEmpty()) {
			b.append("emptydomain");
		} else {
			boolean first = true;
			for (DoubleInterval interval : intervals) {
				if (first) {
					first = false;
				} else {
					b.append("U");
				}
				b.append(interval);
			}
		}
		return b.toString();
	}

	/** �������������������С��һ���� */
	public DoubleInterval jointoOneInterval() {
		if (unknown) {
			return DoubleInterval.fullInterval();
		}
		double tmax = 0, tmin = 0;
		if (intervals.size() > 0) {
			tmax = intervals.last().getMax();
			tmin = intervals.first().getMin();
		} else { // EMPTY
			tmax = Double.NEGATIVE_INFINITY;
			tmin = Double.POSITIVE_INFINITY;			
		}
		return new DoubleInterval(tmin, tmax);
	}
	
	public double getMin(){
		if (intervals.size() > 0) {
			return intervals.first().getMin();
		}else{
			return Double.POSITIVE_INFINITY;
		}
	}
	
	public double getMax(){
		if (intervals.size() > 0) {
			return intervals.last().getMax();
		}else{
			return Double.NEGATIVE_INFINITY;
		}
	}

	/** ����������뵽���У�������������������������ϲ� */
	public void mergeWith(DoubleInterval newInterval) {
		// �ռ����к����������غϵ����䣬������Ӽ�����ɾ��
		if (newInterval.isEmpty()) {
			return;
		}
		List<DoubleInterval> intersectors = new ArrayList<DoubleInterval>();
		List<DoubleInterval> toRemove = new ArrayList<DoubleInterval>();
		for (DoubleInterval interval : intervals) {
			if (interval.isEmpty() || DoubleInterval.canBeJoined(interval, newInterval)) {
				if (!interval.isEmpty()) {
					intersectors.add(interval);
				}
				toRemove.add(interval);
			}
		}
		intervals.removeAll(toRemove);

		// �������������intersectors�е�������кϲ�
		double min = newInterval.getMin();
		double max = newInterval.getMax();

		for (DoubleInterval intersector : intersectors) {
			if (intersector.getMin() < newInterval.getMin()) {
				min = intersector.getMin();
			}
			if (intersector.getMax() > newInterval.getMax()) {
				max = intersector.getMax();
			}
		}
		// ����������
		if(!Double.isNaN(min) && !Double.isNaN(max))
		{
			intervals.add(new DoubleInterval(min, max));
		}
	}

	/** ������������Ľ������֣����ܷ��ؿ����� */
	private static DoubleInterval intersectionOf(DoubleInterval i1, DoubleInterval i2) {
		double new_min;
		double new_max;
		if (i1.getMin() < i2.getMin()) {
			new_min = i2.getMin();
		} else {
			new_min = i1.getMin();
		}

		if (i1.getMax() > i2.getMax()) {
			new_max = i2.getMax();
		} else {
			new_max = i1.getMax();
		}

		return new DoubleInterval(new_min, new_max);
	}
	
	/** ��ѧ���㣺�ӷ� */
	public static DoubleDomain add(DoubleDomain a, DoubleDomain b) {
		DoubleDomain r=null;
		
		if (a.unknown || b.unknown) {
			r = DoubleDomain.getUnknownDomain();
			return r;
		}
		r = new DoubleDomain();
		for (DoubleInterval i1 : a.intervals) {
			for (DoubleInterval i2 : b.intervals) {
				r.mergeWith(DoubleInterval.add(i1, i2));
			}
		}
		return r;
	}

	/** ��ѧ���㣺�ӷ� */
	public static DoubleDomain add(DoubleDomain a, double x) {
		DoubleDomain r=null;
		if (a.unknown) {
			r = DoubleDomain.getUnknownDomain();
			return r;
		}
		r = new DoubleDomain();
		for (DoubleInterval i : a.intervals) {
			r.mergeWith(DoubleInterval.add(i, x));
		}
		return r;
	}

	/** ��ѧ���㣺���� */
	public static DoubleDomain sub(DoubleDomain a, DoubleDomain b) {
		DoubleDomain r=null;
		if (a.unknown || b.unknown) {
			r = DoubleDomain.getUnknownDomain();
			return r;
		}
		r = new DoubleDomain();
		for (DoubleInterval i1 : a.intervals) {
			for (DoubleInterval i2 : b.intervals) {
				r.mergeWith(DoubleInterval.sub(i1, i2));
			}
		}
		return r;
	}

	/** ��ѧ���㣺���� */
	public static DoubleDomain sub(DoubleDomain a, double x) {
		DoubleDomain r=null;
		if (a.unknown) {
			r = DoubleDomain.getUnknownDomain();
			return r;
		}
		r = new DoubleDomain();
		for (DoubleInterval i : a.intervals) {
			r.mergeWith(DoubleInterval.sub(i, x));
		}
		return r;
	}

	/** ��ѧ���㣺�˷� */
	public static DoubleDomain mul(DoubleDomain a, DoubleDomain b) {
		DoubleDomain r=null;
		if (a.unknown || b.unknown) {
			r = DoubleDomain.getUnknownDomain();
			return r;
		}
		r = new DoubleDomain();
		for (DoubleInterval i1 : a.intervals) {
			for (DoubleInterval i2 : b.intervals) {
				r.mergeWith(DoubleInterval.mul(i1, i2));
			}
		}
		return r;
	}

	/** ��ѧ���㣺�˷� */
	public static DoubleDomain mul(DoubleDomain a, double x) {
		DoubleDomain r=null;
		if (a.unknown ) {
			r = DoubleDomain.getUnknownDomain();
			return r;
		}
		r = new DoubleDomain();
		for (DoubleInterval i : a.intervals) {
			r.mergeWith(DoubleInterval.mul(i, x));
		}
		return r;
	}

	/** ��ѧ���㣺���� */
	public static DoubleDomain div(DoubleDomain a, DoubleDomain b) {
		DoubleDomain r=null;
		if (a.unknown || b.unknown) {
			r = DoubleDomain.getUnknownDomain();
			return r;
		}
		r = new DoubleDomain();
		for (DoubleInterval i1 : a.intervals) {
			for (DoubleInterval i2 : b.intervals) {
				r.mergeWith(DoubleInterval.div(i1, i2));
			}
		}
		return r;
	}

	/** ��ѧ���㣺���� */
	public static DoubleDomain div(DoubleDomain a, double x) {
		DoubleDomain r=null;
		if (a.unknown) {
			r = DoubleDomain.getUnknownDomain();
			return r;
		}
		r = new DoubleDomain();
		for (DoubleInterval i : a.intervals) {
			r.mergeWith(DoubleInterval.div(i, x));
		}
		return r;
	}
	
	/** ��ѧ���㣺ȡ�ࣨ����ȷ�� */
	public static DoubleDomain mod(DoubleDomain a, DoubleDomain b) {
		DoubleDomain r=null;
		if (a.unknown || b.unknown) {
			r = DoubleDomain.getUnknownDomain();
			return r;
		}
		r = new DoubleDomain();
		for (DoubleInterval i1 : a.intervals) {
			for (DoubleInterval i2 : b.intervals) {
				r.mergeWith(DoubleInterval.mod(i1, i2));
			}
		}
		return r;
	}

	/** ��ѧ���㣺ȡ�� */
	public static DoubleDomain mod(DoubleDomain a, int x) {
		DoubleDomain r=null;
		if (a.unknown) {
			r = DoubleDomain.getUnknownDomain();
			return r;
		}
		r = new DoubleDomain();
		for (DoubleInterval i : a.intervals) {
			r.mergeWith(DoubleInterval.mod(i, x));
		}
		return r;
	}	

	/** ��ѧ���㣺ȡ����- */
	public static DoubleDomain uminus(DoubleDomain a) {
		DoubleDomain r=null;
		if (a.unknown) {
			r = DoubleDomain.getUnknownDomain();
			return r;
		}

		r = new DoubleDomain();
		for (DoubleInterval i1 : a.intervals) {
			r.mergeWith(DoubleInterval.uminus(i1));
		}
		return r;
	}

	/** ��ѧ���� ��ƽ������sqrt */
	public static DoubleDomain sqrt(DoubleDomain a) {
		DoubleDomain r=null;
		r = new DoubleDomain();
		for (DoubleInterval i1 : a.intervals) {
			r.mergeWith(DoubleInterval.sqrt(i1));
		}
		return r;
	}

	/** ��ѧ���� ��ƽ����sqr */
	public static DoubleDomain sqr(DoubleDomain a) {
		DoubleDomain r=null;
		r = new DoubleDomain();
		for (DoubleInterval i1 : a.intervals) {
			r.mergeWith(DoubleInterval.sqr(i1));
		}
		return r;
	}

	/** ��ѧ���㣺sin */
	public static DoubleDomain sin(DoubleDomain a) {
		DoubleDomain r=null;
		r = new DoubleDomain();
		for (DoubleInterval i1 : a.intervals) {
			r.mergeWith(DoubleInterval.sin(i1));
		}
		return r;
	}

	/** ��ѧ���㣺cos */
	public static DoubleDomain cos(DoubleDomain a) {
		DoubleDomain r=null;
		r = new DoubleDomain();
		for (DoubleInterval i1 : a.intervals) {
			r.mergeWith(DoubleInterval.cos(i1));
		}
		return r;
	}

	/** ��ѧ���㣺tan */
	public static DoubleDomain tan(DoubleDomain a) {
		DoubleDomain r=null;
		r = new DoubleDomain();
		for (DoubleInterval i1 : a.intervals) {
			r.mergeWith(DoubleInterval.tan(i1));
		}
		return r;
	}

	/** ��ѧ���㣺asin */
	public static DoubleDomain asin(DoubleDomain a) {
		DoubleDomain r=null;
		r = new DoubleDomain();
		for (DoubleInterval i1 : a.intervals) {
			r.mergeWith(DoubleInterval.asin(i1));
		}
		return r;
	}

	/** ��ѧ���㣺acos */
	public static DoubleDomain acos(DoubleDomain a) {
		DoubleDomain r=null;
		r = new DoubleDomain();
		for (DoubleInterval i1 : a.intervals) {
			r.mergeWith(DoubleInterval.acos(i1));
		}
		return r;
	}

	/** ��ѧ���㣺atan */
	public static DoubleDomain atan(DoubleDomain a) {
		DoubleDomain r=null;
		r = new DoubleDomain();
		for (DoubleInterval i1 : a.intervals) {
			r.mergeWith(DoubleInterval.atan(i1));
		}
		return r;
	}

	/** ��ѧ���㣺eָ������ */
	public static DoubleDomain exp(DoubleDomain a) {
		DoubleDomain r=null;
		r = new DoubleDomain();
		for (DoubleInterval i1 : a.intervals) {
			r.mergeWith(DoubleInterval.exp(i1));
		}
		return r;
	}

	/** ��ѧ���㣺�������� */
	public static DoubleDomain log(DoubleDomain a) {
		DoubleDomain r=null;
		r = new DoubleDomain();
		for (DoubleInterval i1 : a.intervals) {
			r.mergeWith(DoubleInterval.log(i1));
		}
		return r;
	}

	/** ��ѧ���㣺ָ������ */
	public static DoubleDomain power(DoubleDomain a, DoubleDomain b) {
		DoubleDomain r=null;
		r = new DoubleDomain();
		for (DoubleInterval i1 : a.intervals) {
			if(i1.getMax()<=0)//zys:2010.8.5
				continue;
			for (DoubleInterval i2 : b.intervals) {
				r.mergeWith(DoubleInterval.power(i1, i2));
			}
		}
		return r;
	}

	/** ������ a��b */
	public static DoubleDomain union(DoubleDomain a, DoubleDomain b) {
		DoubleDomain r=null;
		
		if (Config.DOMAIN_CONSERVATIVE) {
			if (a.unknown || b.unknown) {
				r = DoubleDomain.getUnknownDomain();
				return r;
			}
		} else {
			if (a.unknown) {
				r = (DoubleDomain) b.copy();
				return r;
			}
			if (b.unknown) {
				r = (DoubleDomain) a.copy();
				return r;
			}
		}
		
		r = new DoubleDomain(a);
		for (DoubleInterval i : b.intervals) {
			r.mergeWith(i);
		}
		return r;
	}

	/** ������ a��b */
	public static DoubleDomain intersect(DoubleDomain a, DoubleDomain b) {
		DoubleDomain r=null;
		
		if (a.unknown) {
			r = (DoubleDomain) b.copy();
			return r;
		}
		if (b.unknown) {
			r = (DoubleDomain) a.copy();
			return r;
		}
		
		r = new DoubleDomain();
		for (DoubleInterval i1 : a.intervals) {
			for (DoubleInterval i2 : b.intervals) {
				DoubleInterval i3 = intersectionOf(i1, i2);
				r.mergeWith(i3);
			}
		}	
		return r;
	}

	/** ���ϼ��� e-a */
	public static DoubleDomain subtract(DoubleDomain e, DoubleDomain a) {
		DoubleDomain r = inverse(a);
		r = DoubleDomain.intersect(e, r);
		return r;
	}

	/** ����ȡ�� ~a */
	public static DoubleDomain inverse(DoubleDomain a) {
		DoubleDomain r=null;
		
		if (a.unknown) {
			r = DoubleDomain.getUnknownDomain();
			return r;
		}
		
		r= new DoubleDomain();
		if (a.intervals.isEmpty()) {
			r.mergeWith(DoubleInterval.fullInterval());
		} else {
			Iterator interval_i = a.intervals.iterator();
			DoubleInterval first_interval = (DoubleInterval) interval_i.next();
			if (first_interval.getMin() != Double.NEGATIVE_INFINITY) {
				r.intervals.add(new DoubleInterval(Double.NEGATIVE_INFINITY, DoubleMath.prevfp(first_interval.getMin())));
			}
			double last_max = first_interval.getMax();
			while (interval_i.hasNext()) {
				DoubleInterval interval = (DoubleInterval) interval_i.next();
				r.intervals.add(new DoubleInterval(DoubleMath.nextfp(last_max), DoubleMath.prevfp(interval.getMin())));
				last_max = interval.getMax();
			}
			if (last_max != Double.POSITIVE_INFINITY) {
				r.intervals.add(new DoubleInterval(DoubleMath.nextfp(last_max), Double.POSITIVE_INFINITY));
			}
		}
		return r;
	}

	public void setIntervals(TreeSet<DoubleInterval> intervals) {
		this.intervals = intervals;
	}
	public static DoubleDomain valueOf(String str) {
		String[] intervals = str.split("U");
		DoubleDomain domain  = new DoubleDomain();
		for (int i = 0; i < intervals.length; i++) {
			String temp  =intervals[i].replace("[", "");
			temp = temp.replace("]", "");
			int index = temp.indexOf(",");
			double min = 0;
			if (temp.substring(0, index).trim().equals("-inf")) {
				min = DEFAULT_MIN;
			} else {
				min = Double.parseDouble(temp.substring(0, index).trim());
			}
			double max = 0;
			if (temp.substring(index+1).trim().equals("inf")) {
				max = DEFAULT_MAX;
			} else {
				max =Double.parseDouble(temp.substring(index+1).trim());
			}
			DoubleInterval interval = new DoubleInterval(min, max);
			domain.mergeWith(interval);
		}
		return domain;
	}

	@Override
	public Domain intersect(Domain rDomain) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Domain subtract(Domain rDomain) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Domain union(Domain rDomain) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Domain unionwithUnkown(Domain rDomain) {
		// TODO Auto-generated method stub
		return null;
	}
}
